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
import controllers.certificatesofauthority.routes.FinancialOrganisationController
import forms.FinancialOrganisationFormProvider
import models.journeydata.JourneyData
import models.journeydata.certificatesofauthority.FinancialOrganisation.{Bank, BuildingSociety}
import models.journeydata.certificatesofauthority.{CertificatesOfAuthority, FinancialOrganisation}
import models.{CheckMode, NormalMode}
import navigation.{FakeNavigator, Navigator}
import views.html.certificatesofauthority.FinancialOrganisationView
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{atMostOnce, verify, when}
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.Writes
import play.api.mvc.{Call, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import scala.concurrent.Future

class FinancialOrganisationControllerSpec extends SpecBase {

  def onwardRoute: Call = Call("GET", "/foo")

  lazy val routeUrl: String  = FinancialOrganisationController.onPageLoad(NormalMode).url
  lazy val submitUrl: String = FinancialOrganisationController.onSubmit(NormalMode).url

  val formProvider                           = new FinancialOrganisationFormProvider()
  val form: Form[Set[FinancialOrganisation]] = formProvider()

  "FinancialOrganisationController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[FinancialOrganisationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET if no existing answer found" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[FinancialOrganisationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when previously answered" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          certificatesOfAuthority = Some(CertificatesOfAuthority(financialOrganisation = Some(Seq(Bank))))
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[FinancialOrganisationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(Set(Bank)), NormalMode)(
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
            .withFormUrlEncodedBody(("value[0]", ""))

        val boundForm = form.bind(Map("value[0]" -> ""))

        val view = application.injector.instanceOf[FinancialOrganisationView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted and existing section is present" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          certificatesOfAuthority = Some(CertificatesOfAuthority(financialOrganisation = Some(Seq(BuildingSociety))))
        )

      val expectedSection = CertificatesOfAuthority(financialOrganisation = Some(Seq(Bank, BuildingSociety)))

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[CertificatesOfAuthority]], any)
      ) thenReturn Future.successful(expectedSection)

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value[0]", "bank"), ("value[1]", "buildingSociety"))

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[CertificatesOfAuthority]], any)
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the next page when valid data is submitted and existing section is absent" in {

      val expectedSection = CertificatesOfAuthority(financialOrganisation = Some(Seq(Bank, BuildingSociety)))

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
            .withFormUrlEncodedBody(("value[0]", "bank"), ("value[1]", "buildingSociety"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
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
            .withFormUrlEncodedBody(("value[0]", "bank"))

        await(route(application, request).value)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }

    "must render view with CheckMode on a GET" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request = FakeRequest(GET, FinancialOrganisationController.onPageLoad(CheckMode).url)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must submit with CheckMode and redirect" in {

      val expectedSection = CertificatesOfAuthority(financialOrganisation = Some(Seq(Bank)))

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
          FakeRequest(POST, FinancialOrganisationController.onSubmit(CheckMode).url)
            .withFormUrlEncodedBody(("value[0]", "bank"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }
  }
}
