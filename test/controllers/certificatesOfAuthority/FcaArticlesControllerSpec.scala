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

package controllers.certificatesOfAuthority

import base.SpecBase
import controllers.routes
import forms.FcaArticlesFormProvider
import models.FcaArticles.Article14
import models.journeydata.isaproducts.IsaProducts
import models.journeydata.{CertificatesOfAuthority, JourneyData}
import models.{FcaArticles, NormalMode}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{atMostOnce, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.Writes
import play.api.mvc.{Call, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.FcaArticlesView

import scala.concurrent.Future

class FcaArticlesControllerSpec extends SpecBase with MockitoSugar {

  val onwardRoute = "/obligations/enrolment/isa"

  lazy val fcaArticlesRoute = routes.FcaArticlesController.onPageLoad(NormalMode).url

  val formProvider                 = new FcaArticlesFormProvider()
  val form: Form[Set[FcaArticles]] = formProvider()

  "FcaArticles Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request = FakeRequest(GET, fcaArticlesRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[FcaArticlesView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          certificatesOfAuthority = Some(CertificatesOfAuthority(Some(FcaArticles.values), None))
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, fcaArticlesRoute)

        val view = application.injector.instanceOf[FcaArticlesView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(FcaArticles.values.toSet), NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val expectedJourneyData = CertificatesOfAuthority(fcaArticles = Some(Seq(Article14)))

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedJourneyData), any[String], any[String])(any[Writes[CertificatesOfAuthority]], any)
      ) thenReturn Future.successful(expectedJourneyData)

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, fcaArticlesRoute)
            .withFormUrlEncodedBody(("value[0]", FcaArticles.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute
      }
    }

    "must redirect to the next page when valid data is submitted and no existing data was found" in {

      val journeyData =
        JourneyData(groupId = testGroupId, enrolmentId = testString, certificatesOfAuthority = None)

      val expectedJourneyData = CertificatesOfAuthority(fcaArticles = Some(Seq(Article14)))

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedJourneyData), any[String], any[String])(any[Writes[CertificatesOfAuthority]], any)
      ) thenReturn Future.successful(expectedJourneyData)

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, fcaArticlesRoute)
            .withFormUrlEncodedBody(("value[0]", FcaArticles.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute
      }
    }

    "must update existing CertificatesOfAuthority when valid data is submitted" in {

      val existingCertificates =
        CertificatesOfAuthority(
          fcaArticles = None,
          dataItem2 = None
        )

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          certificatesOfAuthority = Some(existingCertificates)
        )

      val expectedUpdated =
        existingCertificates.copy(
          fcaArticles = Some(Seq(Article14))
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedUpdated), any[String], any[String])(any[Writes[CertificatesOfAuthority]], any)
      ).thenReturn(Future.successful(expectedUpdated))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {

        val request =
          FakeRequest(POST, fcaArticlesRoute)
            .withFormUrlEncodedBody(("value[0]", Article14.toString))

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedUpdated), any[String], any[String])(any[Writes[CertificatesOfAuthority]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, fcaArticlesRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[FcaArticlesView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must return Internal Server Error when failed to store data" in {

      val journeyData =
        JourneyData(groupId = testGroupId, enrolmentId = testString, certificatesOfAuthority = None)

      when(
        mockJourneyAnswersService
          .update(any[CertificatesOfAuthority], any[String], any[String])(any[Writes[CertificatesOfAuthority]], any)
      ) thenReturn Future.failed(new Exception)

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, fcaArticlesRoute)
            .withFormUrlEncodedBody(("value[0]", FcaArticles.values.head.toString))

        await(route(application, request).value)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }
  }
}
