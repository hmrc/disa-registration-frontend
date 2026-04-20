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

package controllers.liaisonofficers

import base.SpecBase
import config.FrontendAppConfig
import controllers.liaisonofficers.routes.LiaisonOfficerNameController
import controllers.routes.TaskListController
import forms.LiaisonOfficerNameFormProvider
import models.journeydata.JourneyData
import models.journeydata.liaisonofficers.LiaisonOfficerCommunication.{ByEmail, ByPhone}
import models.journeydata.liaisonofficers.{LiaisonOfficer, LiaisonOfficers}
import models.{CheckMode, NormalMode}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{atMostOnce, never, verify, when}
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.Writes
import play.api.mvc.{Call, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.UuidGenerator
import views.html.liaisonofficers.LiaisonOfficerNameView

import scala.concurrent.Future

class LiaisonOfficerNameControllerSpec extends SpecBase {

  def onwardRoute: Call = Call("GET", "/foo")

  private val existingId  = "existing-id-123"
  private val newId       = "new-id-123"
  private val generatedId = "generated-id-123"

  lazy val routeUrl: String  = LiaisonOfficerNameController.onPageLoad(Some(existingId), NormalMode).url
  lazy val submitUrl: String = LiaisonOfficerNameController.onSubmit(existingId, NormalMode).url

  val formProvider: LiaisonOfficerNameFormProvider = new LiaisonOfficerNameFormProvider()
  val form: Form[String]                           = formProvider()

  val maxLiaisonOfficers: Int = app.injector.instanceOf[FrontendAppConfig].maxLiaisonOfficers

  private def liaisonOfficers(count: Int): Seq[LiaisonOfficer] =
    (1 to count).map(i => LiaisonOfficer(s"id-$i", Some(s"Person $i")))

  "LiaisonOfficerNameController" - {

    "must return OK and the correct view for a GET when an id is provided and no existing answer is found" in {

      when(mockUuidGenerator.generate()).thenReturn(generatedId)

      val application = applicationBuilder(journeyData = Some(emptyJourneyData))
        .overrides(bind[UuidGenerator].toInstance(mockUuidGenerator))
        .build()

      running(application) {
        val request = FakeRequest(GET, LiaisonOfficerNameController.onPageLoad(Some(existingId), NormalMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[LiaisonOfficerNameView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(generatedId, form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when previously answered" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(
            LiaisonOfficers(
              Seq(
                LiaisonOfficer("other-id", Some("Other Person")),
                LiaisonOfficer(existingId, Some(testString))
              )
            )
          )
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, LiaisonOfficerNameController.onPageLoad(Some(existingId), NormalMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[LiaisonOfficerNameView]

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
        val request = FakeRequest(GET, LiaisonOfficerNameController.onPageLoad(None, NormalMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[LiaisonOfficerNameView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(generatedId, form, NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK on GET when editing an existing liaison officer and the maximum number has been reached" in {

      val existingOfficer = LiaisonOfficer(existingId, Some("Existing Person"))
      val others          = liaisonOfficers(maxLiaisonOfficers - 1)

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(
            LiaisonOfficers(existingOfficer +: others)
          )
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request =
          FakeRequest(GET, LiaisonOfficerNameController.onPageLoad(Some(existingId), NormalMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[LiaisonOfficerNameView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(existingId, form.fill("Existing Person"), NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to task list on GET when adding a new liaison officer and the maximum number has been reached" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(
            LiaisonOfficers(liaisonOfficers(maxLiaisonOfficers))
          )
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request =
          FakeRequest(GET, LiaisonOfficerNameController.onPageLoad(None, NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect to task list on GET when an unknown liaison officer id is provided and the maximum number has been reached" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(
            LiaisonOfficers(liaisonOfficers(maxLiaisonOfficers))
          )
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request =
          FakeRequest(GET, LiaisonOfficerNameController.onPageLoad(Some("unknown-id"), NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must return BadRequest and errors when invalid data is submitted" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, LiaisonOfficerNameController.onSubmit(existingId, NormalMode).url)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[LiaisonOfficerNameView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(existingId, boundForm, NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted and existing section contains the same liaison officer id" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(
            LiaisonOfficers(
              Seq(
                LiaisonOfficer("other-id", Some("Other Person")),
                LiaisonOfficer(existingId, Some("Old Name"))
              )
            )
          )
        )

      val expectedSection =
        LiaisonOfficers(
          Seq(
            LiaisonOfficer("other-id", Some("Other Person")),
            LiaisonOfficer(existingId, Some("Updated Name"))
          )
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, LiaisonOfficerNameController.onSubmit(existingId, NormalMode).url)
            .withFormUrlEncodedBody(("value", "Updated Name"))

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must allow updating an existing liaison officer when the maximum number has been reached" in {

      val existingOfficer = LiaisonOfficer(existingId, Some("Old Name"))
      val others          = liaisonOfficers(maxLiaisonOfficers - 1)

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(
            LiaisonOfficers(existingOfficer +: others)
          )
        )

      val expectedSection =
        LiaisonOfficers(
          LiaisonOfficer(existingId, Some("Updated Name")) +: others
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, LiaisonOfficerNameController.onSubmit(existingId, NormalMode).url)
            .withFormUrlEncodedBody(("value", "Updated Name"))

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the next page when valid data is submitted and existing section is present but liaison officer id is new" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(
            LiaisonOfficers(
              Seq(
                LiaisonOfficer("other-id", Some("Other Person"))
              )
            )
          )
        )

      val expectedSection =
        LiaisonOfficers(
          Seq(
            LiaisonOfficer("other-id", Some("Other Person")),
            LiaisonOfficer(newId, Some("New Person"))
          )
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, LiaisonOfficerNameController.onSubmit(newId, NormalMode).url)
            .withFormUrlEncodedBody(("value", "New Person"))

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to task list and not update when trying to add a new liaison officer beyond the maximum" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(
            LiaisonOfficers(liaisonOfficers(maxLiaisonOfficers))
          )
        )

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, LiaisonOfficerNameController.onSubmit(newId, NormalMode).url)
            .withFormUrlEncodedBody(("value", "New Person"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url

        verify(mockJourneyAnswersService, never())
          .update(any[LiaisonOfficers], any[String], any[String])(any[Writes[LiaisonOfficers]], any)
      }
    }

    "must redirect to the next page when valid data is submitted and existing section is absent" in {

      val expectedSection = LiaisonOfficers(Seq(LiaisonOfficer(existingId, Some(testString))))

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, LiaisonOfficerNameController.onSubmit(existingId, NormalMode).url)
            .withFormUrlEncodedBody(("value", testString))

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return Internal Server Error when JourneyAnswersService.update fails" in {

      when(
        mockJourneyAnswersService
          .update(any[LiaisonOfficers], any[String], any[String])(any[Writes[LiaisonOfficers]], any)
      ).thenReturn(Future.failed(new Exception("fubar")))

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, LiaisonOfficerNameController.onSubmit(existingId, NormalMode).url)
            .withFormUrlEncodedBody(("value", testString))

        await(route(application, request).value)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }

    "must preserve existing fields when updating a liaison officer name" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(
            LiaisonOfficers(
              Seq(
                LiaisonOfficer(
                  existingId,
                  fullName = Some("Old Name"),
                  phoneNumber = Some("0123456789"),
                  email = Some("test@test.com"),
                  communication = Set(ByEmail)
                )
              )
            )
          )
        )

      val expectedSection =
        LiaisonOfficers(
          Seq(
            LiaisonOfficer(
              existingId,
              fullName = Some("Updated Name"),
              phoneNumber = Some("0123456789"),
              email = Some("test@test.com"),
              communication = Set(ByEmail)
            )
          )
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, LiaisonOfficerNameController.onSubmit(existingId, NormalMode).url)
            .withFormUrlEncodedBody(("value", "Updated Name"))

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)

        status(result) mustEqual SEE_OTHER
      }
    }

    "must not modify other liaison officers when updating one" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(
            LiaisonOfficers(
              Seq(
                LiaisonOfficer(
                  "other-id",
                  fullName = Some("Other Person"),
                  phoneNumber = Some("0000000000"),
                  email = Some("other@test.com"),
                  communication = Set(ByPhone)
                ),
                LiaisonOfficer(
                  existingId,
                  fullName = Some("Old Name"),
                  phoneNumber = Some("0123456789"),
                  email = Some("test@test.com"),
                  communication = Set(ByEmail)
                )
              )
            )
          )
        )

      val expectedSection =
        LiaisonOfficers(
          Seq(
            LiaisonOfficer(
              "other-id",
              fullName = Some("Other Person"),
              phoneNumber = Some("0000000000"),
              email = Some("other@test.com"),
              communication = Set(ByPhone)
            ),
            LiaisonOfficer(
              existingId,
              fullName = Some("Updated Name"),
              phoneNumber = Some("0123456789"),
              email = Some("test@test.com"),
              communication = Set(ByEmail)
            )
          )
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, LiaisonOfficerNameController.onSubmit(existingId, NormalMode).url)
            .withFormUrlEncodedBody(("value", "Updated Name"))

        route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)
      }
    }

    "must add new liaison officer with default fields when id does not exist" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(
            LiaisonOfficers(Seq.empty)
          )
        )

      val expectedSection =
        LiaisonOfficers(
          Seq(
            LiaisonOfficer(
              newId,
              fullName = Some("New Person"),
              phoneNumber = None,
              email = None,
              communication = Set.empty
            )
          )
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, LiaisonOfficerNameController.onSubmit(newId, NormalMode).url)
            .withFormUrlEncodedBody(("value", "New Person"))

        route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)
      }
    }

    "must render view with CheckMode on a GET" in {
      when(mockUuidGenerator.generate()).thenReturn(generatedId)

      val application = applicationBuilder(journeyData = Some(emptyJourneyData))
        .overrides(bind[UuidGenerator].toInstance(mockUuidGenerator))
        .build()

      running(application) {
        val request = FakeRequest(GET, LiaisonOfficerNameController.onPageLoad(Some(existingId), CheckMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[LiaisonOfficerNameView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(generatedId, form, CheckMode)(request, messages(application)).toString
      }
    }

    "must submit with CheckMode and redirect" in {

      val expectedSection = LiaisonOfficers(Seq(LiaisonOfficer(existingId, Some(testString))))

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, LiaisonOfficerNameController.onSubmit(existingId, CheckMode).url)
            .withFormUrlEncodedBody(("value", testString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }
  }
}
