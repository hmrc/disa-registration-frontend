/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.orgdetails

import base.SpecBase
import models.YesNoAnswer
import models.journeydata.JourneyData
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.checkAnswers.orgDetails.{AddedCorrespondenceAddressSummary, FirmReferenceNumberSummary, OrganisationTelephoneNumberSummary, RegisteredAddressCorrespondenceSummary, RegisteredIsaManagerSummary, TradingNameSummary, TradingUsingDifferentNameSummary, ZReferenceNumberSummary}
import viewmodels.govuk.summarylist.SummaryListViewModel
import views.html.orgdetails.OrganisationDetailsCheckYourAnswersView

class OrganisationDetailsCheckYourAnswersControllerSpec extends SpecBase {

  "OrganisationDetailsCheckYourAnswersController" - {

    "must redirect to Start for if not Business Verified" in {

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData.copy(isaProducts = None))).build()

      running(application) {

        val request =
          FakeRequest(GET, routes.OrganisationDetailsCheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.routes.GrsStartController.onPageLoad().url
      }
    }

    "must return OK and the correct view for a GET" in {

      val jd = JourneyData(
        groupId = testGroupId,
        enrolmentId = testString,
        businessVerification = Some(testBV),
        organisationDetails = Some(completeTaskListOrganisationDetails)
      )

      val orgDetails = jd.organisationDetails

      val application =
        applicationBuilder(journeyData =
          Some(
            emptyJourneyDataWithBusinessVerification.copy(organisationDetails =
              Some(completeTaskListOrganisationDetails)
            )
          )
        ).build()

      running(application) {

        val request =
          FakeRequest(GET, routes.OrganisationDetailsCheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[OrganisationDetailsCheckYourAnswersView]

        val expectedRows =
          Seq(
            RegisteredIsaManagerSummary.row(jd),
            ZReferenceNumberSummary.row(jd),
            TradingUsingDifferentNameSummary.row(jd),
            TradingNameSummary.row(jd),
            FirmReferenceNumberSummary.row(jd),
            RegisteredAddressCorrespondenceSummary.row(jd),
            AddedCorrespondenceAddressSummary
              .row(jd)
              .filter(_ => !orgDetails.flatMap(_.registeredAddressCorrespondence).contains(YesNoAnswer.Yes)),
            OrganisationTelephoneNumberSummary.row(jd)
          ).flatten

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(SummaryListViewModel(expectedRows))(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Task List for a GET if no existing Org Details data is found" in {

      val application =
        applicationBuilder(journeyData =
          Some(emptyJourneyDataWithBusinessVerification.copy(organisationDetails = None))
        ).build()

      running(application) {

        val request =
          FakeRequest(GET, routes.OrganisationDetailsCheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.routes.TaskListController.onPageLoad().url
      }
    }

    "must redirect to Start for a GET if no existing data is found" in {

      val application =
        applicationBuilder(journeyData = None).build()

      running(application) {

        val request =
          FakeRequest(GET, routes.OrganisationDetailsCheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.routes.GrsStartController.onPageLoad().url
      }
    }
  }
}
