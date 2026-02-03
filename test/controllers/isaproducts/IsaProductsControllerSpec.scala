/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.isaproducts

import base.SpecBase
import controllers.isaproducts.routes.IsaProductsController
import forms.IsaProductsFormProvider
import models.NormalMode
import models.journeydata.JourneyData
import models.journeydata.isaproducts.IsaProduct.CashIsas
import models.journeydata.isaproducts.{IsaProduct, IsaProducts}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.Writes
import play.api.mvc.{Call, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.isaproducts.IsaProductsView
import org.mockito.ArgumentMatchers.{eq => eqTo}

import scala.concurrent.Future

class IsaProductsControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  lazy val isaProductsRoute: String = IsaProductsController.onPageLoad(NormalMode).url

  val formProvider                = new IsaProductsFormProvider()
  val form: Form[Set[IsaProduct]] = formProvider()

  "IsaProduct Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request = FakeRequest(GET, isaProductsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[IsaProductsView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET if no existing journey data found" in {
      val application = applicationBuilder(journeyData = None).build()

      running(application) {
        val request = FakeRequest(GET, isaProductsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[IsaProductsView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          isaProducts = Some(IsaProducts(Some(IsaProduct.values), None))
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, isaProductsRoute)

        val view = application.injector.instanceOf[IsaProductsView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(IsaProduct.values.toSet), NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val expectedJourneyData = IsaProducts(Some(Seq(CashIsas)))

      when(
        mockJourneyAnswersService.update(eqTo(expectedJourneyData), any[String])(any[Writes[IsaProducts]], any)
      ) thenReturn Future.successful(expectedJourneyData)

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, isaProductsRoute)
            .withFormUrlEncodedBody(("value[0]", IsaProduct.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the next page when valid data is submitted and no existing data was found" in {

      val expectedJourneyData = IsaProducts(Some(Seq(CashIsas)))

      when(
        mockJourneyAnswersService.update(eqTo(expectedJourneyData), any[String])(any[Writes[IsaProducts]], any)
      ) thenReturn Future.successful(expectedJourneyData)

      val application =
        applicationBuilder(journeyData = None)
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, isaProductsRoute)
            .withFormUrlEncodedBody(("value[0]", IsaProduct.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, isaProductsRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[IsaProductsView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must return Internal Server Error when invalid data is submitted" in {

      when(
        mockJourneyAnswersService.update(any[IsaProducts], any[String])(any[Writes[IsaProducts]], any)
      ) thenReturn Future.failed(new Exception)

      val application =
        applicationBuilder(journeyData = None)
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, isaProductsRoute)
            .withFormUrlEncodedBody(("value[0]", IsaProduct.values.head.toString))

        await(route(application, request).value)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }
  }
}
