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
import forms.FirmReferenceNumberFormProvider
import models.NormalMode
import models.journeydata.{JourneyData, OrganisationDetails}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.Writes
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.orgdetails.FirmReferenceNumberView

import scala.concurrent.Future

class FirmReferenceNumberControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  val formProvider             = new FirmReferenceNumberFormProvider()
  val form: Form[String]       = formProvider()
  val fcaNumber                = "123456"
  val journeyData: JourneyData =
    JourneyData(
      groupId = testGroupId,
      organisationDetails = Some(OrganisationDetails(fcaNumber = Some(fcaNumber)))
    )

  lazy val firmReferenceNumberRoute: String =
    controllers.orgdetails.routes.FirmReferenceNumberController.onPageLoad(NormalMode).url

  "FirmReferenceNumber Controller" - {

    "must return OK and correctly load the FirmReferenceNumber page" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request = FakeRequest(GET, firmReferenceNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[FirmReferenceNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "on pageLoad must populate the FirmReferenceNumber page when the question has previously been answered" in {

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, firmReferenceNumberRoute)

        val view = application.injector.instanceOf[FirmReferenceNumberView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(fcaNumber), NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      when(
        mockJourneyAnswersService.update(any[OrganisationDetails], any[String])(any[Writes[OrganisationDetails]], any)
      ) thenReturn Future.successful(())

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, firmReferenceNumberRoute)
            .withFormUrlEncodedBody(("value", fcaNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, firmReferenceNumberRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[FirmReferenceNumberView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must return Internal Server Error when theres an issue updating the journey answers" in {

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
          FakeRequest(POST, firmReferenceNumberRoute)
            .withFormUrlEncodedBody(("value", fcaNumber))

        val result = route(application, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
        contentAsString(result) must include(messages.messages("journeyRecovery.continue.title"))
      }
    }

  }
}
