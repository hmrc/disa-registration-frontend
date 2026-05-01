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

package controllers.thirdparty

import base.SpecBase
import controllers.thirdparty.routes.ThirdPartyOrgDetailsController
import controllers.routes._
import forms.ThirdPartyOrgDetailsFormProvider
import models.YesNoAnswer.Yes
import models.journeydata.JourneyData
import models.journeydata.thirdparty.*
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
import views.html.thirdparty.ThirdPartyOrgDetailsView

import scala.concurrent.Future

class ThirdPartyOrgDetailsControllerSpec extends SpecBase {

  private val existingId  = "existing-id-123"
  private val newId       = "new-id-123"
  private val generatedId = "generated-id-123"

  lazy val formProvider: ThirdPartyOrgDetailsFormProvider =
    new ThirdPartyOrgDetailsFormProvider()

  lazy val form: Form[ThirdPartyOrgDetailsForm] = formProvider()

  def onwardRoute(id: String) =
    Call("GET", s"/obligations/enrolment/isa/isa-returns-managed-by-third-party?id=$id")

  "ThirdPartyOrgDetailsController onPageLoad" - {

    "must return OK and empty form when no existing data and id provided" in {

      when(mockUuidGenerator.generate()).thenReturn(generatedId)

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .overrides(bind[UuidGenerator].toInstance(mockUuidGenerator))
          .build()

      running(application) {
        val request =
          FakeRequest(GET, ThirdPartyOrgDetailsController.onPageLoad(Some(existingId), NormalMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ThirdPartyOrgDetailsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(generatedId, form, NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate form when existing third party is found" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          thirdPartyOrganisations = Some(
            ThirdPartyOrganisations(
              managedByThirdParty = None,
              thirdParties = Seq(
                ThirdParty(existingId, Some("Old Name"), thirdPartyFrn = Some("123456"))
              ),
              connectedOrganisations = Set.empty
            )
          )
        )

      val application =
        applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request =
          FakeRequest(GET, ThirdPartyOrgDetailsController.onPageLoad(Some(existingId), NormalMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ThirdPartyOrgDetailsView]

        val filledForm =
          form.fill(
            ThirdPartyOrgDetailsForm(
              "Old Name",
              Some("123456")
            )
          )

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(existingId, filledForm, NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to TaskListController when no id is provided and max third-parties reached" in {

      val max = 2

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          thirdPartyOrganisations = Some(
            ThirdPartyOrganisations(
              managedByThirdParty = None,
              thirdParties = Seq(
                ThirdParty(existingId, Some("Old Name"), thirdPartyFrn = Some("123456")),
                ThirdParty(newId, Some("Old Name"), thirdPartyFrn = Some("123457"))
              ),
              connectedOrganisations = Set.empty
            )
          )
        )

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .configure("max-third-parties" -> max)
          .build()

      running(application) {
        val request =
          FakeRequest(GET, ThirdPartyOrgDetailsController.onPageLoad(None, NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }
  }

  "ThirdPartyOrgDetailsController - onSubmit" - {

    "must return BAD_REQUEST when invalid data is submitted" in {

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, ThirdPartyOrgDetailsController.onSubmit(existingId, NormalMode).url)
            .withFormUrlEncodedBody(
              "thirdPartyName" -> "",
              "frn"            -> "abc"
            )

        val boundForm = form.bind(
          Map(
            "thirdPartyName" -> "",
            "frn"            -> "abc"
          )
        )

        val result = route(application, request).value

        val view = application.injector.instanceOf[ThirdPartyOrgDetailsView]

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(existingId, boundForm, NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must update existing third party" in {
      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          thirdPartyOrganisations = Some(
            ThirdPartyOrganisations(
              Some(Yes),
              Seq(
                ThirdParty(existingId, Some("Old Name"), thirdPartyFrn = Some("123456"))
              ),
              Set.empty
            )
          )
        )

      val expected =
        ThirdPartyOrganisations(
          Some(Yes),
          Seq(
            ThirdParty(existingId, Some("Updated Name"), thirdPartyFrn = Some("654321"))
          ),
          Set.empty
        )

      when(
        mockJourneyAnswersService.update(eqTo(expected), any[String], any[String])(
          any[Writes[ThirdPartyOrganisations]],
          any
        )
      ).thenReturn(Future.successful(expected))

      val application =
        applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, ThirdPartyOrgDetailsController.onSubmit(existingId, NormalMode).url)
            .withFormUrlEncodedBody(
              "thirdPartyName" -> "Updated Name",
              "frn"            -> "654321"
            )

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expected), any[String], any[String])(any[Writes[ThirdPartyOrganisations]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute(existingId).url
      }
    }

    "must add new third party when id does not exist" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          thirdPartyOrganisations = Some(
            ThirdPartyOrganisations(None, Seq.empty, Set.empty)
          )
        )

      val expected =
        ThirdPartyOrganisations(
          None,
          Seq(
            ThirdParty(newId, Some("New Name"), thirdPartyFrn = Some("123456"))
          ),
          Set.empty
        )

      when(
        mockJourneyAnswersService.update(eqTo(expected), any[String], any[String])(
          any[Writes[ThirdPartyOrganisations]],
          any
        )
      ).thenReturn(Future.successful(expected))

      val application =
        applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, ThirdPartyOrgDetailsController.onSubmit(newId, NormalMode).url)
            .withFormUrlEncodedBody(
              "thirdPartyName" -> "New Name",
              "frn"            -> "123456"
            )

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expected), any[String], any[String])(any[Writes[ThirdPartyOrganisations]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute(newId).url
      }
    }

    "must return Internal Server Error when JourneyAnswersService.update fails" in {

      when(mockJourneyAnswersService.update(any(), any(), any())(any(), any()))
        .thenReturn(Future.failed(new Exception("Update journeyAnswersService failed - Service Down")))

      val journeyData =
        emptyJourneyData.copy(
          thirdPartyOrganisations = Some(
            ThirdPartyOrganisations(
              None,
              Seq(ThirdParty(existingId, Some("Existing"))),
              Set.empty
            )
          )
        )

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, ThirdPartyOrgDetailsController.onSubmit(existingId, NormalMode).url)
            .withFormUrlEncodedBody(
              "thirdPartyName" -> testString,
              "frn"            -> "123456"
            )

        val result = route(application, request).value

        await(result)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }

    "must render CheckMode view" in {

      when(mockUuidGenerator.generate()).thenReturn(generatedId)

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .overrides(bind[UuidGenerator].toInstance(mockUuidGenerator))
          .build()

      running(application) {
        val request =
          FakeRequest(GET, ThirdPartyOrgDetailsController.onPageLoad(Some(existingId), CheckMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ThirdPartyOrgDetailsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(generatedId, form, CheckMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect in CheckMode on submit" in {

      val expected =
        ThirdPartyOrganisations(
          None,
          Seq(ThirdParty(existingId, Some("Name"), thirdPartyFrn = None)),
          Set.empty
        )

      when(
        mockJourneyAnswersService.update(eqTo(expected), any[String], any[String])(
          any[Writes[ThirdPartyOrganisations]],
          any
        )
      ).thenReturn(Future.successful(expected))

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, ThirdPartyOrgDetailsController.onSubmit(existingId, CheckMode).url)
            .withFormUrlEncodedBody(
              "thirdPartyName" -> "Name",
              "frn"            -> ""
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
      }
    }
  }
}
