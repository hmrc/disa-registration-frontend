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
import controllers.signatories.routes.SignatoryNameController
import controllers.routes.TaskListController
import forms.SignatoryNameFormProvider
import models.journeydata.JourneyData
import models.journeydata.signatories.{Signatories, Signatory}
import models.{CheckMode, NormalMode}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{atMostOnce, verify, when}
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.Writes
import play.api.mvc.{Call, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.UuidGenerator
import views.html.signatories.SignatoryNameView

import scala.concurrent.Future

class SignatoryNameControllerSpec extends SpecBase {

  private val existingId  = "existing-id-123"
  private val newId       = "new-id-123"
  private val generatedId = "generated-id-123"

  def onwardRoute(id: String): Call = Call("GET", s"/obligations/enrolment/isa/signatory-job-title?id=$id")

  lazy val routeUrl: String  = SignatoryNameController.onPageLoad(Some(existingId), NormalMode).url
  lazy val submitUrl: String = SignatoryNameController.onSubmit(existingId, NormalMode).url

  val formProvider: SignatoryNameFormProvider = new SignatoryNameFormProvider()
  val form: Form[String]                      = formProvider()

  "SignatoryNameController" - {

    "must return OK and the correct view for a GET when an id is provided and no existing answer is found" in {

      when(mockUuidGenerator.generate()).thenReturn(generatedId)

      val application = applicationBuilder(journeyData = Some(emptyJourneyData))
        .overrides(bind[UuidGenerator].toInstance(mockUuidGenerator))
        .build()

      running(application) {
        val request = FakeRequest(GET, SignatoryNameController.onPageLoad(Some(existingId), NormalMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SignatoryNameView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(generatedId, form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when previously answered" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(
            Signatories(
              Seq(
                Signatory("other-id", Some("Other Person")),
                Signatory(existingId, Some(testString))
              )
            )
          )
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, SignatoryNameController.onPageLoad(Some(existingId), NormalMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SignatoryNameView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(existingId, form.fill(testString), NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET when no id is provided" in {

      when(mockUuidGenerator.generate()).thenReturn(generatedId)

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .overrides(bind[UuidGenerator].toInstance(mockUuidGenerator))
          .build()

      running(application) {
        val request = FakeRequest(GET, SignatoryNameController.onPageLoad(None, NormalMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SignatoryNameView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(generatedId, form, NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return BadRequest and errors when invalid data is submitted" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, SignatoryNameController.onSubmit(existingId, NormalMode).url)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[SignatoryNameView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(existingId, boundForm, NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted and existing section contains the same signatory id" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(
            Signatories(
              Seq(
                Signatory("other-id", Some("Other Person")),
                Signatory(existingId, Some("Old Name"))
              )
            )
          )
        )

      val expectedSection =
        Signatories(
          Seq(
            Signatory("other-id", Some("Other Person")),
            Signatory(existingId, Some("Updated Name"))
          )
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[Signatories]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, SignatoryNameController.onSubmit(existingId, NormalMode).url)
            .withFormUrlEncodedBody(("value", "Updated Name"))

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[Signatories]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute(existingId).url
      }
    }

    "must redirect to the next page when valid data is submitted and existing section is present but signatory id is new" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(
            Signatories(
              Seq(
                Signatory("other-id", Some("Other Person"))
              )
            )
          )
        )

      val expectedSection =
        Signatories(
          Seq(
            Signatory("other-id", Some("Other Person")),
            Signatory(newId, Some("New Person"))
          )
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[Signatories]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, SignatoryNameController.onSubmit(newId, NormalMode).url)
            .withFormUrlEncodedBody(("value", "New Person"))

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[Signatories]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute(newId).url
      }
    }

    "must redirect to the next page when valid data is submitted and existing section is absent" in {

      val expectedSection = Signatories(Seq(Signatory(existingId, Some(testString))))

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[Signatories]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, SignatoryNameController.onSubmit(existingId, NormalMode).url)
            .withFormUrlEncodedBody(("value", testString))

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[Signatories]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute(existingId).url
      }
    }

    "must return Internal Server Error when JourneyAnswersService.update fails" in {

      when(
        mockJourneyAnswersService
          .update(any[Signatories], any[String], any[String])(any[Writes[Signatories]], any)
      ).thenReturn(Future.failed(new Exception("fubar")))

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, SignatoryNameController.onSubmit(existingId, NormalMode).url)
            .withFormUrlEncodedBody(("value", testString))

        await(route(application, request).value)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }

    "must render view with CheckMode on a GET" in {
      when(mockUuidGenerator.generate()).thenReturn(generatedId)

      val application = applicationBuilder(journeyData = Some(emptyJourneyData))
        .overrides(bind[UuidGenerator].toInstance(mockUuidGenerator))
        .build()

      running(application) {
        val request = FakeRequest(GET, SignatoryNameController.onPageLoad(Some(existingId), CheckMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SignatoryNameView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(generatedId, form, CheckMode)(request, messages(application)).toString
      }
    }

    "must submit with CheckMode and redirect" in {

      val expectedSection = Signatories(Seq(Signatory(existingId, Some(testString))))

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[Signatories]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, SignatoryNameController.onSubmit(existingId, CheckMode).url)
            .withFormUrlEncodedBody(("value", testString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual s"/obligations/enrolment/isa/check-added-signatory?id=$existingId"
      }
    }

    "must preserve existing fields when updating a signatory name" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(
            Signatories(
              Seq(
                Signatory(existingId, Some("Old Name"), jobTitle = Some("Director"))
              )
            )
          )
        )

      val expectedSection =
        Signatories(
          Seq(
            Signatory(existingId, Some("Updated Name"), jobTitle = Some("Director"))
          )
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[Signatories]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, SignatoryNameController.onSubmit(existingId, NormalMode).url)
            .withFormUrlEncodedBody(("value", "Updated Name"))

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[Signatories]], any)

        status(result) mustEqual SEE_OTHER
      }
    }

    "must not modify other signatories when updating one" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(
            Signatories(
              Seq(
                Signatory("other-id", Some("Other Person"), jobTitle = Some("CEO")),
                Signatory(existingId, Some("Old Name"), jobTitle = Some("Director"))
              )
            )
          )
        )

      val expectedSection =
        Signatories(
          Seq(
            Signatory("other-id", Some("Other Person"), jobTitle = Some("CEO")),
            Signatory(existingId, Some("Updated Name"), jobTitle = Some("Director"))
          )
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[Signatories]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, SignatoryNameController.onSubmit(existingId, NormalMode).url)
            .withFormUrlEncodedBody(("value", "Updated Name"))

        route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[Signatories]], any)
      }
    }

    "must add new signatory with default fields when id does not exist" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(
            Signatories(Seq.empty)
          )
        )

      val expectedSection =
        Signatories(
          Seq(Signatory(newId, Some("New Person"), jobTitle = None))
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[Signatories]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, SignatoryNameController.onSubmit(newId, NormalMode).url)
            .withFormUrlEncodedBody(("value", "New Person"))

        route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[Signatories]], any)
      }
    }
    "must redirect to TaskListController when no id is provided and max signatories reached" in {

      val max = 2

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(
            Signatories(
              Seq(
                Signatory("id-1", Some("Person One")),
                Signatory("id-2", Some("Person Two"))
              )
            )
          )
        )

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .configure("max-signatories" -> max)
          .build()

      running(application) {
        val request =
          FakeRequest(GET, SignatoryNameController.onPageLoad(None, NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }
    "must allow access when below max signatories and no id is provided" in {

      val max = 3

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(
            Signatories(
              Seq(Signatory("id-1", Some("Person One")))
            )
          )
        )

      when(mockUuidGenerator.generate()).thenReturn(generatedId)

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(bind[UuidGenerator].toInstance(mockUuidGenerator))
          .configure("max-signatories" -> max)
          .build()

      running(application) {
        val request =
          FakeRequest(GET, SignatoryNameController.onPageLoad(None, NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }
  }
}
