/*
 * Copyright 2026 HM Revenue & Customs
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
import controllers.orgdetails.routes.OrganisationTelephoneNumberController
import forms.OrganisationTelephoneNumberFormProvider
import models.journeydata.{JourneyData, OrganisationDetails}
import models.{CheckMode, NormalMode}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{atMostOnce, verify, when}
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.Writes
import play.api.mvc.{Call, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.orgdetails.OrganisationTelephoneNumberView

import scala.concurrent.Future

class OrganisationTelephoneNumberControllerSpec extends SpecBase {

  def onwardRoute: Call = Call("GET", "/foo")

  lazy val routeUrl: String  = OrganisationTelephoneNumberController.onPageLoad(NormalMode).url
  lazy val submitUrl: String = OrganisationTelephoneNumberController.onSubmit(NormalMode).url

  val formProvider       = new OrganisationTelephoneNumberFormProvider()
  val form: Form[String] = formProvider()

  private val testTelNo = "12345678910"

  "OrganisationTelephoneNumberController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[OrganisationTelephoneNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET if no existing answer found" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[OrganisationTelephoneNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when previously answered" in {

      val journeyData =
        emptyJourneyData.copy(organisationDetails = Some(OrganisationDetails(orgTelephoneNumber = Some(testTelNo))))

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[OrganisationTelephoneNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(testTelNo), NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return BadRequest and errors when invalid data is submitted" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[OrganisationTelephoneNumberView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted and existing section is present" in {

      val expectedSection = OrganisationDetails(orgTelephoneNumber = Some(testTelNo))
      val journeyData     = emptyJourneyData.copy(organisationDetails = Some(expectedSection))

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[OrganisationDetails]], any)
      ) thenReturn Future.successful(expectedSection)

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", testTelNo))

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[OrganisationDetails]], any)
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the next page when valid data is submitted and existing section is absent" in {

      val expectedSection = OrganisationDetails(orgTelephoneNumber = Some(testTelNo))

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[OrganisationDetails]], any)
      ) thenReturn Future.successful(expectedSection)

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", testTelNo))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return Internal Server Error when JourneyAnswersService.update fails" in {

      when(
        mockJourneyAnswersService
          .update(any[OrganisationDetails], any[String], any[String])(any[Writes[OrganisationDetails]], any)
      ) thenReturn Future.failed(new Exception("fubar"))

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", testTelNo))

        await(route(application, request).value)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }

    "must render view with CheckMode on a GET" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request = FakeRequest(GET, OrganisationTelephoneNumberController.onPageLoad(CheckMode).url)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must submit with CheckMode and redirect" in {

      val expectedSection = OrganisationDetails(orgTelephoneNumber = Some(testTelNo))

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[OrganisationDetails]], any)
      ) thenReturn Future.successful(expectedSection)

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, OrganisationTelephoneNumberController.onSubmit(CheckMode).url)
            .withFormUrlEncodedBody(("value", testTelNo))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }
  }
}
