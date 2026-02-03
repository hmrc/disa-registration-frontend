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

package controllers.orgdetails

import base.SpecBase
import forms.ZReferenceNumberFormProvider
import models.NormalMode
import models.journeydata.isaproducts.IsaProducts
import models.journeydata.{JourneyData, OrganisationDetails}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.Writes
import play.api.mvc.{Call, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.orgdetails.ZReferenceNumberView

import scala.concurrent.Future

class ZReferenceNumberControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  val formProvider: ZReferenceNumberFormProvider = new ZReferenceNumberFormProvider()
  val form: Form[String]                         = formProvider()

  lazy val zReferenceNumberRoute: String = routes.ZReferenceNumberController.onPageLoad(NormalMode).url

  "ZReferenceNumber Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request = FakeRequest(GET, zReferenceNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ZReferenceNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          organisationDetails = Some(OrganisationDetails(zRefNumber = Some("zRef")))
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, zReferenceNumberRoute)

        val view = application.injector.instanceOf[ZReferenceNumberView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("zRef"), NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val expectedJourneyData = OrganisationDetails(zRefNumber = Some(testZRef))

      when(
        mockJourneyAnswersService.update(eqTo(expectedJourneyData), any[String])(any[Writes[OrganisationDetails]], any)
      ) thenReturn Future.successful(expectedJourneyData)

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, zReferenceNumberRoute)
            .withFormUrlEncodedBody(("value", testZRef))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the next page when valid data is submitted and no existing data was found" in {

      val expectedJourneyData = OrganisationDetails(zRefNumber = Some(testZRef))

      when(
        mockJourneyAnswersService.update(eqTo(expectedJourneyData), any[String])(any[Writes[OrganisationDetails]], any)
      ) thenReturn Future.successful(expectedJourneyData)

      val application =
        applicationBuilder(journeyData = None)
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, zReferenceNumberRoute)
            .withFormUrlEncodedBody(("value", testZRef))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, zReferenceNumberRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[ZReferenceNumberView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must return Internal Server Error when updateJourneyAnswers returns a fail exception" in {

      when(
        mockJourneyAnswersService.update(any[OrganisationDetails], any[String])(any[Writes[OrganisationDetails]], any)
      ) thenReturn Future.failed(new Exception)

      val application =
        applicationBuilder(journeyData = None)
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, zReferenceNumberRoute)
            .withFormUrlEncodedBody(("value", "Z1234"))

        await(route(application, request).value)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }
  }
}
