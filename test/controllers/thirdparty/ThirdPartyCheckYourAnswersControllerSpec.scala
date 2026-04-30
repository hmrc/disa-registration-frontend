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
import controllers.routes.TaskListController
import controllers.thirdparty.routes.ThirdPartyCheckYourAnswersController
import models.YesNoAnswer.{No, Yes}
import models.journeydata.JourneyData
import models.journeydata.thirdparty.{ThirdParty, ThirdPartyOrganisations}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.checkAnswers.thirdparty.*
import viewmodels.govuk.summarylist.*
import views.html.thirdparty.ThirdPartyCheckYourAnswersView

class ThirdPartyCheckYourAnswersControllerSpec extends SpecBase {

  private val existingId = "existing-id-123"
  private val otherId    = "other-id-123"

  private def routeUrl(id: String) =
    ThirdPartyCheckYourAnswersController.onPageLoad(id).url

  private def buildJourneyData(thirdParties: Seq[ThirdParty]) =
    JourneyData(
      groupId = testGroupId,
      enrolmentId = testString,
      thirdPartyOrganisations = Some(
        ThirdPartyOrganisations(
          managedByThirdParty = Some(Yes),
          thirdParties = thirdParties
        )
      )
    )

  "ThirdPartyCheckYourAnswersController" - {

    "must return OK and the correct view when third party is complete" in {

      val thirdParty =
        ThirdParty(
          id = existingId,
          thirdPartyName = Some("Test Org"),
          managingIsaReturns = Some(Yes),
          usingInvestorFunds = Some(No),
          investorFundsPercentage = Some("50")
        )

      val journeyData = buildJourneyData(
        Seq(
          ThirdParty(otherId, Some("Other Org"), Some("123"), Some(Yes), Some(No), Some("20")),
          thirdParty
        )
      )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl(existingId))
        val result  = route(application, request).value

        val view = application.injector.instanceOf[ThirdPartyCheckYourAnswersView]

        val expectedRows =
          Seq(
            ThirdPartyOrgDetailsSummary.row(thirdParty, 2),
            ReturnsManagedByThirdPartySummary.row(thirdParty),
            InvestorFundsUsedByThirdPartySummary.row(thirdParty),
            ThirdPartyInvestorFundsPercentageSummary.row(thirdParty)
          ).flatten

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(SummaryListViewModel(expectedRows))(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect when third party is not found" in {

      val journeyData = buildJourneyData(Seq.empty)

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl(existingId))
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect when third party is in progress (missing name)" in {

      val thirdParty =
        ThirdParty(
          id = existingId,
          thirdPartyName = None,
          managingIsaReturns = Some(Yes),
          usingInvestorFunds = Some(No)
        )

      val application =
        applicationBuilder(journeyData = Some(buildJourneyData(Seq(thirdParty)))).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl(existingId))
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect when third party is in progress (missing managingIsaReturns)" in {

      val thirdParty =
        ThirdParty(
          id = existingId,
          thirdPartyName = Some("Test Org"),
          managingIsaReturns = None,
          usingInvestorFunds = Some(No)
        )

      val application =
        applicationBuilder(journeyData = Some(buildJourneyData(Seq(thirdParty)))).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl(existingId))
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect when third party is in progress (missing usingInvestorFunds)" in {

      val thirdParty =
        ThirdParty(
          id = existingId,
          thirdPartyName = Some("Test Org"),
          managingIsaReturns = Some(Yes),
          usingInvestorFunds = None
        )

      val application =
        applicationBuilder(journeyData = Some(buildJourneyData(Seq(thirdParty)))).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl(existingId))
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect when all required fields are missing (in progress)" in {

      val thirdParty =
        ThirdParty(
          id = existingId,
          thirdPartyName = None,
          managingIsaReturns = None,
          usingInvestorFunds = None
        )

      val application =
        applicationBuilder(journeyData = Some(buildJourneyData(Seq(thirdParty)))).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl(existingId))
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }
  }
}
