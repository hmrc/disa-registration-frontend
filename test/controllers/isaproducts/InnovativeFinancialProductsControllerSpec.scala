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
import forms.InnovativeFinancialProductsFormProvider
import models.NormalMode
import models.journeydata.JourneyData
import models.journeydata.isaproducts.InnovativeFinancialProduct.PeertopeerLoansAndHave36hPermissions
import models.journeydata.isaproducts.{InnovativeFinancialProduct, IsaProducts}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.Writes
import play.api.mvc.{Call, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.isaproducts.InnovativeFinancialProductsView

import scala.concurrent.Future

class InnovativeFinancialProductsControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  lazy val innovativeFinancialProductsRoute: String =
    routes.InnovativeFinancialProductsController.onPageLoad(NormalMode).url

  val journeyData: JourneyData =
    JourneyData(
      groupId = testGroupId,
      enrolmentId = testString,
      isaProducts = Some(IsaProducts(None, None, None, Some(InnovativeFinancialProduct.values)))
    )

  val formProvider                                = new InnovativeFinancialProductsFormProvider()
  val form: Form[Set[InnovativeFinancialProduct]] = formProvider()

  "InnovativeFinancialProducts Controller" - {

    "must return OK and correctly load the InnovativeFinancialProducts page" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request = FakeRequest(GET, innovativeFinancialProductsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[InnovativeFinancialProductsView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "on pageLoad must populate the InnovativeFinancialProducts page correctly when the question has previously been answered" in {

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, innovativeFinancialProductsRoute)

        val view = application.injector.instanceOf[InnovativeFinancialProductsView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(InnovativeFinancialProduct.values.toSet), NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val expectedJourneyData =
        IsaProducts(innovativeFinancialProducts = Some(Seq(PeertopeerLoansAndHave36hPermissions)))

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedJourneyData), any[String], any[String])(any[Writes[IsaProducts]], any)
      ) thenReturn Future.successful(expectedJourneyData)

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, innovativeFinancialProductsRoute)
            .withFormUrlEncodedBody(("value[0]", InnovativeFinancialProduct.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the next page when valid data is submitted and no existing data was found" in {

      val expectedJourneyData =
        IsaProducts(innovativeFinancialProducts = Some(Seq(PeertopeerLoansAndHave36hPermissions)))

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedJourneyData), any[String], any[String])(any[Writes[IsaProducts]], any)
      ) thenReturn Future.successful(expectedJourneyData)

      val application =
        applicationBuilder(journeyData = None)
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, innovativeFinancialProductsRoute)
            .withFormUrlEncodedBody(("value[0]", InnovativeFinancialProduct.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, innovativeFinancialProductsRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[InnovativeFinancialProductsView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must return Internal Server Error when theres an issue updating the journey answers" in {

      when(
        mockJourneyAnswersService.update(any[IsaProducts], any[String], any[String])(any[Writes[IsaProducts]], any)
      ) thenReturn Future.failed(new Exception)

      val application =
        applicationBuilder(journeyData = None)
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, innovativeFinancialProductsRoute)
            .withFormUrlEncodedBody(("value[0]", InnovativeFinancialProduct.values.head.toString))

        await(route(application, request).value)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }
  }
}
