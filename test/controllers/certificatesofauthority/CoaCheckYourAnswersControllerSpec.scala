/*
 * Copyright 2023 HM Revenue & Customs
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
import models.journeydata.JourneyData
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.checkAnswers.{CertificatesOfAuthorityYesNoSummary, FcaArticlesSummary, FinancialOrganisationSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.certificatesofauthority.CoaCheckYourAnswersView

class CoaCheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency {

  "CoaCheckYourAnswersControllerSpec Controller" - {

    "must return OK and correctly load the check your answers page - when certificatesYesNo = Yes " in {
      val journeyData: JourneyData = testJourneyData.copy(certificatesOfAuthority = Some(testCoaAnswersWithArticles))

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {

        val request = FakeRequest(GET, routes.CoaCheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CoaCheckYourAnswersView]
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(SummaryListViewModel(expectedSummaryRows(journeyData)))(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and correctly load the check your answers page - when certificatesYesNo = No" in {

      val journeyData: JourneyData =
        testJourneyData.copy(certificatesOfAuthority = Some(testCoaAnswersWithFinancialOrg))

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {

        val request = FakeRequest(GET, routes.CoaCheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CoaCheckYourAnswersView]
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(SummaryListViewModel(expectedSummaryRows(journeyData)))(
          request,
          messages(application)
        ).toString
      }
    }
  }

  def expectedSummaryRows(journeyData: JourneyData): Seq[SummaryListRow] = Seq(
    CertificatesOfAuthorityYesNoSummary.row(journeyData),
    FcaArticlesSummary.row(journeyData),
    FinancialOrganisationSummary.row(journeyData)
  ).flatten

}
