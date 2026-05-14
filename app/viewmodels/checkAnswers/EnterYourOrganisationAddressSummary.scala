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

package viewmodels.checkAnswers

import controllers.orgdetails.routes.EnterYourOrganisationAddressController
import models.CheckMode
import models.journeydata.CorrespondenceAddress
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object EnterYourOrganisationAddressSummary {

  def row(answer: CorrespondenceAddress)(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = "enterYourOrganisationAddress.checkYourAnswersLabel",
      value = ValueViewModel(
        HtmlContent(
          Seq(
            answer.addressLine1,
            answer.addressLine2,
            answer.addressLine3,
            answer.postCode
          ).flatten
            .filter(_.nonEmpty)
            .map(line => HtmlFormat.escape(line).toString)
            .mkString("<br>")
        )
      ),
      actions = Seq(
        ActionItemViewModel("site.change", EnterYourOrganisationAddressController.onPageLoad(CheckMode).url)
          .withVisuallyHiddenText(messages("enterYourOrganisationAddress.change.hidden"))
      )
    )
}
