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
import controllers.certificatesofauthority.routes.CertificatesOfAuthorityYesNoController
import models.CheckMode
import models.journeydata.JourneyData
import models.journeydata.certificatesofauthority.{CertificatesOfAuthority, CertificatesOfAuthorityYesNo}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}
import viewmodels.checkAnswers.CertificatesOfAuthorityYesNoSummary
import viewmodels.govuk.all.{ActionItemViewModel, FluentActionItem, SummaryListRowViewModel, ValueViewModel}

class CertificatesOfAuthorityYesNoSummarySpec extends SpecBase {

  "CertificatesOfAuthorityYesNoSummary.row" - {

    "must return None when certificatesOfAuthority is missing" in {
      CertificatesOfAuthorityYesNoSummary.row(emptyJourneyData) mustBe None
    }

    "must return None when certificatesYesNo is missing" in {
      val answers: JourneyData =
        emptyJourneyData.copy(certificatesOfAuthority = Some(CertificatesOfAuthority(None)))

      CertificatesOfAuthorityYesNoSummary.row(answers) mustBe None
    }

    CertificatesOfAuthorityYesNo.values.foreach { answer =>
      s"must return a SummaryListRow when answered $answer" in {

        val answers: JourneyData =
          emptyJourneyData.copy(certificatesOfAuthority = Some(CertificatesOfAuthority(Some(answer))))

        val expected: SummaryListRow =
          SummaryListRowViewModel(
            key = Key(Text(messages("certificatesOfAuthorityYesNo.checkYourAnswersLabel"))),
            value = ValueViewModel(
              HtmlContent(
                HtmlFormat.escape(messages(s"certificatesOfAuthorityYesNo.$answer"))
              )
            ),
            actions = Seq(
              ActionItemViewModel(
                Text(messages("site.change")),
                CertificatesOfAuthorityYesNoController.onPageLoad(CheckMode).url
              ).withVisuallyHiddenText(messages("certificatesOfAuthorityYesNo.change.hidden"))
            )
          )

        CertificatesOfAuthorityYesNoSummary.row(answers).value mustBe expected
      }
    }
  }
}
