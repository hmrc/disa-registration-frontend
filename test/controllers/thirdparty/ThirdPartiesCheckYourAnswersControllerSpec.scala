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
import controllers.thirdparty.routes.*
import models.NormalMode
import models.YesNoAnswer.Yes
import models.journeydata.JourneyData
import models.journeydata.thirdparty.{ThirdParty, ThirdPartyOrganisations}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.thirdparty.ThirdPartiesCheckYourAnswersView

class ThirdPartiesCheckYourAnswersControllerSpec extends SpecBase {

  private def routeUrl =
    routes.ThirdPartiesCheckYourAnswersController.onPageLoad().url

  private def buildJourney(thirdParties: Seq[ThirdParty]) =
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

  "ThirdPartiesCheckYourAnswersController" - {

    "must redirect to task list when no third parties exist" in {

      val application =
        applicationBuilder(journeyData = Some(buildJourney(Seq.empty))).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect when only one completed third party exists" in {

      val singleCompleteTp =
        ThirdParty(
          id = "tp-1",
          thirdPartyName = Some("Test Org"),
          thirdPartyFrn = Some("FRN123"),
          managingIsaReturns = Some(Yes),
          usingInvestorFunds = Some(Yes),
          investorFundsPercentage = Some("50")
        )

      val application =
        applicationBuilder(journeyData = Some(buildJourney(Seq(singleCompleteTp)))).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual AddedThirdPartiesController.onPageLoad(NormalMode, None).url
      }
    }

    "must redirect when all third parties are in progress" in {

      val inProgressTp =
        ThirdParty(
          id = "tp-1",
          thirdPartyName = None,
          managingIsaReturns = None,
          usingInvestorFunds = None
        )

      val application =
        applicationBuilder(journeyData = Some(buildJourney(Seq(inProgressTp)))).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must return OK when more than one completed third party exists" in {

      val tp1 =
        ThirdParty(
          id = "tp-1",
          thirdPartyName = Some("Org 1"),
          thirdPartyFrn = Some("FRN1"),
          managingIsaReturns = Some(Yes),
          usingInvestorFunds = Some(Yes),
          investorFundsPercentage = Some("40")
        )

      val tp2 =
        ThirdParty(
          id = "tp-2",
          thirdPartyName = Some("Org 2"),
          thirdPartyFrn = Some("FRN2"),
          managingIsaReturns = Some(Yes),
          usingInvestorFunds = Some(Yes),
          investorFundsPercentage = Some("60")
        )

      val journey = buildJourney(Seq(tp1, tp2))

      val application =
        applicationBuilder(journeyData = Some(journey)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)
        val result  = route(application, request).value

        val view = application.injector.instanceOf[ThirdPartiesCheckYourAnswersView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(journey.thirdPartyOrganisations.get)(
          request,
          messages(application)
        ).toString
      }
    }
  }
}
