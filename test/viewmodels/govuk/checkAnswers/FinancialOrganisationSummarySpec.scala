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

package viewmodels.govuk.checkAnswers

import base.SpecBase
import controllers.certificatesofauthority.routes.FinancialOrganisationController
import models.CheckMode
import models.journeydata.JourneyData
import models.journeydata.certificatesofauthority.CertificatesOfAuthority
import models.journeydata.certificatesofauthority.FinancialOrganisation.Bank
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}
import viewmodels.checkAnswers.FinancialOrganisationSummary
import viewmodels.govuk.all.{ActionItemViewModel, FluentActionItem, SummaryListRowViewModel, ValueViewModel}

class FinancialOrganisationSummarySpec extends SpecBase {

  "FinancialOrganisationSummary.row" - {

    "must return None when certificatesOfAuthority is missing" in {
      FinancialOrganisationSummary.row(emptyJourneyData) mustBe None
    }

    "must return None when financialOrganisation is missing" in {
      val answers: JourneyData =
        emptyJourneyData.copy(certificatesOfAuthority = Some(CertificatesOfAuthority(None)))

      FinancialOrganisationSummary.row(answers) mustBe None
    }

    "must return a SummaryListRow when answered" in {

      val answers: JourneyData =
        emptyJourneyData.copy(certificatesOfAuthority =
          Some(CertificatesOfAuthority(financialOrganisation = Some(Seq(Bank))))
        )

      val expected: SummaryListRow =
        SummaryListRowViewModel(
          key = Key(Text(messages("financialOrganisation.checkYourAnswersLabel"))),
          value = ValueViewModel(
            HtmlContent(
              HtmlFormat.escape(messages(s"financialOrganisation.bank"))
            )
          ),
          actions = Seq(
            ActionItemViewModel(
              Text(messages("site.change")),
              FinancialOrganisationController.onPageLoad(CheckMode).url
            ).withVisuallyHiddenText(messages("financialOrganisation.change.hidden"))
          )
        )

      FinancialOrganisationSummary.row(answers).value mustBe expected
    }
  }
}
