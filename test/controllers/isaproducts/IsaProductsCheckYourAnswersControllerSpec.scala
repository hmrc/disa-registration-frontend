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

package controllers.isaproducts

import base.SpecBase
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.checkAnswers.isaproducts.{InnovativeFinancialProductsSummary, IsaProductsSummary, PeerToPeerPlatformNumberSummary, PeerToPeerPlatformSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.isaproducts.IsaProductsCheckYourAnswersView

class IsaProductsCheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency {

  val testSummaryRows: Seq[SummaryListRow] = Seq(
    IsaProductsSummary.row(testJourneyData),
    InnovativeFinancialProductsSummary.row(testJourneyData),
    PeerToPeerPlatformSummary.row(testJourneyData),
    PeerToPeerPlatformNumberSummary.row(testJourneyData)
  ).flatten

  "IsaProductsCheckYourAnswersController Controller" - {

    "must return OK and correctly load the check your answers page" in {

      val application = applicationBuilder(journeyData = Some(testJourneyData)).build()

      running(application) {

        val request = FakeRequest(GET, routes.IsaProductsCheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[IsaProductsCheckYourAnswersView]
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(SummaryListViewModel(testSummaryRows))(
          request,
          messages(application)
        ).toString
      }
    }
  }
}
