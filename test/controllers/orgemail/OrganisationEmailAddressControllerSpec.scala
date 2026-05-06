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

package controllers.orgemail

import base.SpecBase
import connectors.EmailVerificationConnector
import controllers.orgemail.routes.OrganisationEmailAddressController
import forms.OrganisationEmailAddressFormProvider
import models.journeydata.{JourneyData, OrganisationEmail}
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
import uk.gov.hmrc.http.HeaderCarrier
import views.html.orgemail.OrganisationEmailAddressView

import scala.concurrent.Future

class OrganisationEmailAddressControllerSpec extends SpecBase {

  def onwardRoute: Call = Call("GET", "/foo")

  private val oldEmail = "old@example.com"
  private val newEmail = "new@example.com"

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(mockEmailVerificationConnector.sendCode(any[String])(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))
  }

  lazy val routeUrl: String =
    OrganisationEmailAddressController.onPageLoad(NormalMode).url

  lazy val submitUrl: String =
    OrganisationEmailAddressController.onSubmit(NormalMode).url

  val formProvider: OrganisationEmailAddressFormProvider =
    new OrganisationEmailAddressFormProvider()

  val form: Form[String] =
    formProvider()

  "OrganisationEmailAddressController" - {

    "must return OK and the correct view for a GET when the question has not previously been answered" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          organisationEmail = Some(OrganisationEmail())
        )

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(bind[EmailVerificationConnector].toInstance(mockEmailVerificationConnector))
          .build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[OrganisationEmailAddressView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when previously answered" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          organisationEmail = Some(
            OrganisationEmail(
              organisationEmail = Some(oldEmail),
              verified = Some(true)
            )
          )
        )

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(bind[EmailVerificationConnector].toInstance(mockEmailVerificationConnector))
          .build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[OrganisationEmailAddressView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(oldEmail), NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return BadRequest and errors when blank data is submitted" in {

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .overrides(bind[EmailVerificationConnector].toInstance(mockEmailVerificationConnector))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[OrganisationEmailAddressView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return BadRequest and errors when an invalid email address is submitted" in {

      val invalidEmail = "not-an-email"

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .overrides(bind[EmailVerificationConnector].toInstance(mockEmailVerificationConnector))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", invalidEmail))

        val boundForm = form.bind(Map("value" -> invalidEmail))

        val view = application.injector.instanceOf[OrganisationEmailAddressView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must send verification code, update the section, and redirect to the next page when valid data is submitted" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString
        )

      val expectedSection =
        OrganisationEmail(
          organisationEmail = Some(newEmail),
          verified = Some(false)
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[OrganisationEmail]], any)
      ).thenReturn(Future.successful(expectedSection))

      when(mockEmailVerificationConnector.sendCode(eqTo(newEmail))(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[EmailVerificationConnector].toInstance(mockEmailVerificationConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", newEmail))

        val result = route(application, request).value

        verify(mockEmailVerificationConnector, atMostOnce)
          .sendCode(eqTo(newEmail))(any[HeaderCarrier])

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[OrganisationEmail]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must reset verified to false when a different valid email is submitted" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          organisationEmail = Some(
            OrganisationEmail(
              organisationEmail = Some(oldEmail),
              verified = Some(true)
            )
          )
        )

      val expectedSection =
        OrganisationEmail(
          organisationEmail = Some(newEmail),
          verified = Some(false)
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[OrganisationEmail]], any)
      ).thenReturn(Future.successful(expectedSection))

      when(mockEmailVerificationConnector.sendCode(eqTo(newEmail))(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[EmailVerificationConnector].toInstance(mockEmailVerificationConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", newEmail))

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[OrganisationEmail]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return Internal Server Error when sending the verification code fails" in {

      when(mockEmailVerificationConnector.sendCode(eqTo(newEmail))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("email verification failed")))

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[EmailVerificationConnector].toInstance(mockEmailVerificationConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", newEmail))

        await(route(application, request).value)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }

    "must return Internal Server Error when JourneyAnswersService.update fails" in {

      when(mockEmailVerificationConnector.sendCode(eqTo(newEmail))(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      when(
        mockJourneyAnswersService
          .update(any[OrganisationEmail], any[String], any[String])(any[Writes[OrganisationEmail]], any)
      ).thenReturn(Future.failed(new Exception("fubar")))

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[EmailVerificationConnector].toInstance(mockEmailVerificationConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", newEmail))

        await(route(application, request).value)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }

    "must render view with CheckMode on a GET" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          organisationEmail = Some(
            OrganisationEmail(
              organisationEmail = Some(oldEmail),
              verified = Some(true)
            )
          )
        )

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(bind[EmailVerificationConnector].toInstance(mockEmailVerificationConnector))
          .build()

      running(application) {
        val request =
          FakeRequest(GET, OrganisationEmailAddressController.onPageLoad(CheckMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[OrganisationEmailAddressView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(oldEmail), CheckMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must submit with CheckMode and redirect" in {

      val expectedSection =
        OrganisationEmail(
          organisationEmail = Some(newEmail),
          verified = Some(false)
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[OrganisationEmail]], any)
      ).thenReturn(Future.successful(expectedSection))

      when(mockEmailVerificationConnector.sendCode(eqTo(newEmail))(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[EmailVerificationConnector].toInstance(mockEmailVerificationConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, OrganisationEmailAddressController.onSubmit(CheckMode).url)
            .withFormUrlEncodedBody(("value", newEmail))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }
  }
}
