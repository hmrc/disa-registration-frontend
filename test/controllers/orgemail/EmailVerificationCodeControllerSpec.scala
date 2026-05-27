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
import controllers.orgemail.routes.{EmailVerificationCodeController, OrganisationEmailAddressController}
import forms.EmailVerificationCodeFormProvider
import models.NormalMode
import models.emailverification.VerifyEmailCodeResult
import models.journeydata.{JourneyData, OrganisationEmail}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{atMostOnce, never, verify, when}
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.Writes
import play.api.mvc.{Call, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import views.html.orgemail.EmailVerificationCodeView

import scala.concurrent.Future

class EmailVerificationCodeControllerSpec extends SpecBase {

  def onwardRoute: Call = Call("GET", "/foo")

  private val email     = "old@example.com"
  private val validCode = "ABCDEF"

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(mockEmailVerificationConnector.verifyCode(any[String], any[String])(any[HeaderCarrier]))
      .thenReturn(Future.successful(VerifyEmailCodeResult.Verified))

    when(mockEmailVerificationConnector.sendCode(any[String])(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))
  }

  lazy val routeUrl: String =
    EmailVerificationCodeController.onPageLoad(NormalMode, None).url

  lazy val submitUrl: String =
    EmailVerificationCodeController.onSubmit(NormalMode, None).url

  lazy val requestNewCodeUrl: String =
    EmailVerificationCodeController.requestNewCode(NormalMode).url

  val formProvider: EmailVerificationCodeFormProvider =
    new EmailVerificationCodeFormProvider()

  val form: Form[String] =
    formProvider()

  private val journeyDataWithEmail =
    JourneyData(
      groupId = testGroupId,
      enrolmentId = testString,
      organisationEmail = Some(
        OrganisationEmail(
          organisationEmail = Some(email),
          verified = Some(false)
        )
      )
    )

  "EmailVerificationCodeController" - {

    "must return OK and the correct view for a GET when organisation email exists" in {

      val application =
        applicationBuilder(journeyData = Some(journeyDataWithEmail))
          .overrides(bind[EmailVerificationConnector].toInstance(mockEmailVerificationConnector))
          .build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EmailVerificationCodeView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, None, email)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the organisation email address page for a GET when organisation email does not exist" in {

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

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual OrganisationEmailAddressController.onPageLoad(NormalMode).url
      }
    }

    "must verify the code, update the section, and redirect to the next page when valid data is submitted" in {

      val expectedSection =
        OrganisationEmail(
          organisationEmail = Some(email),
          verified = Some(true)
        )

      when(mockEmailVerificationConnector.verifyCode(eqTo(email), eqTo(validCode))(any[HeaderCarrier]))
        .thenReturn(Future.successful(VerifyEmailCodeResult.Verified))

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[OrganisationEmail]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyDataWithEmail))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[EmailVerificationConnector].toInstance(mockEmailVerificationConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", validCode))

        val result = route(application, request).value

        verify(mockEmailVerificationConnector, atMostOnce())
          .verifyCode(eqTo(email), eqTo(validCode))(any[HeaderCarrier])

        verify(mockJourneyAnswersService, atMostOnce())
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[OrganisationEmail]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return BadRequest with invalid code error when email verification fails" in {

      when(mockEmailVerificationConnector.verifyCode(eqTo(email), eqTo(validCode))(any[HeaderCarrier]))
        .thenReturn(Future.successful(VerifyEmailCodeResult.InvalidCode))

      val application =
        applicationBuilder(journeyData = Some(journeyDataWithEmail))
          .overrides(bind[EmailVerificationConnector].toInstance(mockEmailVerificationConnector))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", validCode))

        val expectedForm =
          form.withError("value", "emailVerificationCode.error.invalid")

        val view = application.injector.instanceOf[EmailVerificationCodeView]

        val result = route(application, request).value

        verify(mockEmailVerificationConnector, atMostOnce())
          .verifyCode(eqTo(email), eqTo(validCode))(any[HeaderCarrier])

        verify(mockJourneyAnswersService, never)
          .update(any[OrganisationEmail], any[String], any[String])(any[Writes[OrganisationEmail]], any)

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(expectedForm, NormalMode, None, email)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the organisation email address page when submitted but organisation email does not exist" in {

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
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", validCode))

        val result = route(application, request).value

        verify(mockEmailVerificationConnector, never)
          .verifyCode(any[String], any[String])(any[HeaderCarrier])

        verify(mockJourneyAnswersService, never)
          .update(any[OrganisationEmail], any[String], any[String])(any[Writes[OrganisationEmail]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual OrganisationEmailAddressController.onPageLoad(NormalMode).url
      }
    }

    "must return Internal Server Error when verifying the code fails unexpectedly" in {

      when(mockEmailVerificationConnector.verifyCode(eqTo(email), eqTo(validCode))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("email verification failed")))

      val application =
        applicationBuilder(journeyData = Some(journeyDataWithEmail))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[EmailVerificationConnector].toInstance(mockEmailVerificationConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", validCode))

        await(route(application, request).value)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }

    "must return Internal Server Error when JourneyAnswersService.update fails" in {

      val expectedSection =
        OrganisationEmail(
          organisationEmail = Some(email),
          verified = Some(true)
        )

      when(mockEmailVerificationConnector.verifyCode(eqTo(email), eqTo(validCode))(any[HeaderCarrier]))
        .thenReturn(Future.successful(VerifyEmailCodeResult.Verified))

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[OrganisationEmail]], any)
      ).thenReturn(Future.failed(new Exception("fubar")))

      val application =
        applicationBuilder(journeyData = Some(journeyDataWithEmail))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[EmailVerificationConnector].toInstance(mockEmailVerificationConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", validCode))

        await(route(application, request).value)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }

    "must send a new verification code and redirect back to the code page when requestNewCode is submitted" in {

      val application =
        applicationBuilder(journeyData = Some(journeyDataWithEmail))
          .overrides(bind[EmailVerificationConnector].toInstance(mockEmailVerificationConnector))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, requestNewCodeUrl)

        val result = route(application, request).value

        verify(mockEmailVerificationConnector, atMostOnce())
          .sendCode(eqTo(email))(any[HeaderCarrier])

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual EmailVerificationCodeController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to the organisation email address page when requestNewCode is submitted but organisation email does not exist" in {

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
        val request =
          FakeRequest(POST, requestNewCodeUrl)

        val result = route(application, request).value

        verify(mockEmailVerificationConnector, never)
          .sendCode(any[String])(any[HeaderCarrier])

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual OrganisationEmailAddressController.onPageLoad(NormalMode).url
      }
    }

    "must return Internal Server Error when requesting a new code fails" in {

      when(mockEmailVerificationConnector.sendCode(eqTo(email))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("email verification failed")))

      val application =
        applicationBuilder(journeyData = Some(journeyDataWithEmail))
          .overrides(bind[EmailVerificationConnector].toInstance(mockEmailVerificationConnector))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, requestNewCodeUrl)

        await(route(application, request).value)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }
  }
}
