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

package controllers.signatories

import base.SpecBase
import controllers.routes.IndexController
import controllers.signatories.routes.RemoveSignatoryController
import forms.YesNoAnswerFormProvider
import models.YesNoAnswer.{No, Yes}
import models.journeydata.JourneyData
import models.journeydata.signatories.{Signatories, Signatory}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{atMostOnce, verify, when}
import play.api.data.Form
import play.api.libs.json.Writes
import play.api.mvc.{Call, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.signatories.RemoveSignatoryView

import scala.concurrent.Future

class RemoveSignatoryControllerSpec extends SpecBase {

  def onwardRoute: Call = Call("GET", "/obligations/enrolment/isa")

  private val existingId   = "existing-id-123"
  private val otherId      = "other-id-123"
  private val existingName = "Jane Smith"

  lazy val routeUrl: String  = RemoveSignatoryController.onPageLoad(existingId).url
  lazy val submitUrl: String = RemoveSignatoryController.onSubmit(existingId).url

  val formProvider: YesNoAnswerFormProvider = new YesNoAnswerFormProvider()
  val form: Form[models.YesNoAnswer]        = formProvider("removeSignatory.error.required")

  private val existingSignatory =
    Signatory(
      id = existingId,
      fullName = Some(existingName),
      jobTitle = Some("Job title")
    )

  private val otherSignatory =
    Signatory(
      id = otherId,
      fullName = Some("Other Person"),
      jobTitle = Some("Job title")
    )

  "RemoveSignatoryController" - {

    "must return OK and the correct view for a GET when the signatory exists" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(Signatories(Seq(existingSignatory, otherSignatory)))
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemoveSignatoryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(existingId, existingName, form)(request, messages(application)).toString
      }
    }

    "must redirect to Index on a GET when the signatory does not exist" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(Signatories(Seq(otherSignatory)))
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual IndexController.onPageLoad().url
      }
    }

    "must redirect to Index on a GET when the section is absent" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual IndexController.onPageLoad().url
      }
    }

    "must return BadRequest and errors when invalid data is submitted" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(Signatories(Seq(existingSignatory, otherSignatory)))
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> "")

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[RemoveSignatoryView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(existingId, existingName, boundForm)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when Yes is submitted and the signatory is removed" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(Signatories(Seq(existingSignatory, otherSignatory)))
        )

      val expectedSection =
        Signatories(Seq(otherSignatory))

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[Signatories]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> Yes.toString)

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[Signatories]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the next page when No is submitted and the section is unchanged" in {

      val existingSection =
        Signatories(Seq(existingSignatory, otherSignatory))

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(existingSection)
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(existingSection), any[String], any[String])(any[Writes[Signatories]], any)
      ).thenReturn(Future.successful(existingSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> No.toString)

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(existingSection), any[String], any[String])(any[Writes[Signatories]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the next page when Yes is submitted and removing the signatory leaves an empty section" in {

      val existingSection =
        Signatories(Seq(existingSignatory))

      val updatedSection =
        Signatories(Seq.empty)

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(existingSection)
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(updatedSection), any[String], any[String])(any[Writes[Signatories]], any)
      ).thenReturn(Future.successful(updatedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> Yes.toString)

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(updatedSection), any[String], any[String])(any[Writes[Signatories]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to Index when data is submitted and the signatory does not exist" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(Signatories(Seq(otherSignatory)))
        )

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> Yes.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual IndexController.onPageLoad().url
      }
    }

    "must redirect to Index when data is submitted and the section is absent" in {

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> Yes.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual IndexController.onPageLoad().url
      }
    }

    "must redirect to Index when Yes is submitted but signatory is not found during update" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(Signatories(Seq(existingSignatory)))
        )

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {

        val missingId = "missing-id"

        val request =
          FakeRequest(POST, RemoveSignatoryController.onSubmit(missingId).url)
            .withFormUrlEncodedBody("value" -> Yes.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual IndexController.onPageLoad().url
      }
    }

    "must return Internal Server Error when JourneyAnswersService.update fails" in {

      val existingSection =
        Signatories(Seq(existingSignatory, otherSignatory))

      val updatedSection =
        Signatories(Seq(otherSignatory))

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(existingSection)
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(updatedSection), any[String], any[String])(any[Writes[Signatories]], any)
      ).thenReturn(Future.failed(new Exception("fubar")))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> Yes.toString)

        await(route(application, request).value)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }
  }
}
