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
import controllers.signatories.routes.SignatoryCheckYourAnswersController
import controllers.routes.TaskListController
import models.journeydata.JourneyData
import models.journeydata.signatories.{Signatories, Signatory}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.checkAnswers.signatories.{SignatoryJobTitleSummary, SignatoryNameSummary}
import viewmodels.govuk.summarylist.*
import views.html.signatories.SignatoryCheckYourAnswersView

class SignatoryCheckYourAnswersControllerSpec extends SpecBase {

  private val existingId = "existing-id-123"
  private val otherId    = "other-id-123"

  lazy val routeUrl: String = SignatoryCheckYourAnswersController.onPageLoad(existingId).url

  private def buildJourneyData(signatories: Seq[Signatory]) =
    JourneyData(
      groupId = testGroupId,
      enrolmentId = testString,
      signatories = Some(Signatories(signatories))
    )

  "SignatoryCheckYourAnswersController" - {

    "must return OK and the correct view when signatory has both fields defined" in {

      val signatory =
        Signatory(
          id = existingId,
          fullName = Some("Jane Smith"),
          jobTitle = Some("Job Title")
        )

      val journeyData = buildJourneyData(
        Seq(
          Signatory(otherId, Some("Other Person"), Some("Other Job")),
          signatory
        )
      )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)
        val result  = route(application, request).value

        val view = application.injector.instanceOf[SignatoryCheckYourAnswersView]

        val expectedRows =
          Seq(
            SignatoryNameSummary.row(signatory),
            SignatoryJobTitleSummary.row(signatory)
          ).flatten

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(SummaryListViewModel(expectedRows))(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect when signatory is not found" in {

      val journeyData = buildJourneyData(Seq.empty)

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect when fullName is None" in {

      val signatory =
        Signatory(
          id = existingId,
          fullName = None,
          jobTitle = Some("Job Title")
        )

      val application =
        applicationBuilder(journeyData = Some(buildJourneyData(Seq(signatory)))).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect when jobTitle is None" in {

      val signatory =
        Signatory(
          id = existingId,
          fullName = Some("Jane Smith"),
          jobTitle = None
        )

      val application =
        applicationBuilder(journeyData = Some(buildJourneyData(Seq(signatory)))).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect when both fields are None" in {

      val signatory =
        Signatory(
          id = existingId,
          fullName = None,
          jobTitle = None
        )

      val application =
        applicationBuilder(journeyData = Some(buildJourneyData(Seq(signatory)))).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }
  }
}
