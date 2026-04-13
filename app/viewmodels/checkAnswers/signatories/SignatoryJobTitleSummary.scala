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

package viewmodels.checkAnswers.signatories

import controllers.signatories.routes.SignatoryJobTitleController
import models.CheckMode
import models.journeydata.signatories.Signatory
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object SignatoryJobTitleSummary {

  def row(signatory: Signatory)(implicit messages: Messages): Option[SummaryListRow] =
    signatory.jobTitle.map { answer =>
      SummaryListRowViewModel(
        key = "signatoryJobTitle.checkYourAnswersLabel",
        value = ValueViewModel(HtmlFormat.escape(answer).toString),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            SignatoryJobTitleController.onPageLoad(signatory.id, CheckMode).url
          )
            .withVisuallyHiddenText(messages("signatoryJobTitle.change.hidden"))
        )
      )
    }
}
