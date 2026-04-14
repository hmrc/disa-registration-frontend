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

import controllers.signatories.routes.{RemoveSignatoryController, SignatoryNameController}
import models.CheckMode
import models.journeydata.signatories.Signatory
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object AddedSignatorySummary {

  def row(signatory: Signatory)(implicit messages: Messages): Option[SummaryListRow] =
    signatory.fullName.map { answer =>
      SummaryListRowViewModel(
        key = KeyViewModel(HtmlFormat.escape(answer).toString).withCssClass("govuk-!-font-weight-regular"),
        value = ValueViewModel("").withCssClass("govuk-!-width-one-quarter"),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            SignatoryNameController.onPageLoad(Some(signatory.id), CheckMode).url
          ).withVisuallyHiddenText(messages("signatoryName.change.hidden")),
          ActionItemViewModel(
            "site.remove",
            RemoveSignatoryController.onPageLoad(signatory.id).url,
          ).withVisuallyHiddenText(messages("signatoryName.change.hidden"))
        )
      )
    }
}
