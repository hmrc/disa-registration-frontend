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

package controllers.certificatesofauthority

import base.SpecBase
import controllers.certificatesofauthority.routes.CertificatesOfAuthorityYesNoController
import forms.CertificatesOfAuthorityYesNoFormProvider
import models.{CheckMode, NormalMode}
import models.journeydata.JourneyData
import models.journeydata.certificatesofauthority.CertificatesOfAuthorityYesNo.Yes
import models.journeydata.certificatesofauthority.{CertificatesOfAuthority, CertificatesOfAuthorityYesNo}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.Writes
import play.api.mvc.{Call, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.certificatesofauthority.CertificatesOfAuthorityYesNoView

import scala.concurrent.Future

class CertificatesOfAuthorityYesNoControllerSpec extends SpecBase {

  def onwardRoute: Call = Call("GET", "/foo")

  lazy val routeUrl: String  = CertificatesOfAuthorityYesNoController.onPageLoad(NormalMode).url
  lazy val submitUrl: String = CertificatesOfAuthorityYesNoController.onSubmit(NormalMode).url

  val formProvider                             = new CertificatesOfAuthorityYesNoFormProvider()
  val form: Form[CertificatesOfAuthorityYesNo] = formProvider()

  "CertificatesOfAuthorityYesNoController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CertificatesOfAuthorityYesNoView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when previously answered" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          certificatesOfAuthority = Some(CertificatesOfAuthority(certificatesYesNo = Some(Yes)))
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CertificatesOfAuthorityYesNoView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(Yes), NormalMode)(request, messages(application)).toString
      }
    }

    "must return BadRequest and errors when invalid data is submitted" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", "not sure"))

        val boundForm = form.bind(Map("value" -> "not sure"))
        val view      = application.injector.instanceOf[CertificatesOfAuthorityYesNoView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    CertificatesOfAuthorityYesNo.values.foreach { answer =>
      s"must redirect to the next page when valid data ($answer) is submitted" in {

        val expectedSection = CertificatesOfAuthority(certificatesYesNo = Some(answer))

        when(
          mockJourneyAnswersService
            .update(eqTo(expectedSection), any[String], any[String])(any[Writes[CertificatesOfAuthority]], any)
        ) thenReturn Future.successful(expectedSection)

        val application =
          applicationBuilder(journeyData = Some(emptyJourneyData))
            .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, submitUrl)
              .withFormUrlEncodedBody(("value", answer.toString))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      s"must redirect to the next page when valid data ($answer) is submitted and no existing data was found" in {

        val expectedSection = CertificatesOfAuthority(certificatesYesNo = Some(answer))

        when(
          mockJourneyAnswersService
            .update(eqTo(expectedSection), any[String], any[String])(any[Writes[CertificatesOfAuthority]], any)
        ) thenReturn Future.successful(expectedSection)

        val application =
          applicationBuilder(journeyData = Some(emptyJourneyData))
            .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, submitUrl)
              .withFormUrlEncodedBody(("value", answer.toString))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }
    }

    "must return Internal Server Error when JourneyAnswersService.update fails" in {
      when(
        mockJourneyAnswersService
          .update(any[CertificatesOfAuthority], any[String], any[String])(any[Writes[CertificatesOfAuthority]], any)
      ) thenReturn Future.failed(new Exception("fubar"))

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", Yes.toString))

        await(route(application, request).value)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }

    "must render view with CheckMode on GET" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request = FakeRequest(GET, CertificatesOfAuthorityYesNoController.onPageLoad(CheckMode).url)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must submit with CheckMode and redirect" in {

      val expectedSection = CertificatesOfAuthority(certificatesYesNo = Some(Yes))

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[CertificatesOfAuthority]], any)
      ) thenReturn Future.successful(expectedSection)

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, CertificatesOfAuthorityYesNoController.onSubmit(CheckMode).url)
            .withFormUrlEncodedBody(("value", Yes.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }
  }
}
