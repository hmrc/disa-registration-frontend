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

package viewmodels.checkAnswers.thirdparty

import controllers.thirdparty.routes.{ProductsManagedByThirdPartyController, ReturnsManagedByThirdPartyController}
import models.CheckMode
import models.journeydata.thirdparty.{ThirdParty, ThirdPartyOrganisations}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object ProductsManagedByThirdPartySummary {

  def row(thirdPartyOrganisations: ThirdPartyOrganisations)(implicit messages: Messages): Option[SummaryListRow] =
    for {
      answer <- thirdPartyOrganisations.managedByThirdParty
    } yield SummaryListRowViewModel(
      key = Key(Text(messages("productsManagedByThirdParty.checkYourAnswersLabel"))),
      value = ValueViewModel(
        HtmlContent(
          HtmlFormat.escape(messages(s"site.$answer"))
        )
      ),
      actions = Seq(
        ActionItemViewModel(
          "site.change",
          ProductsManagedByThirdPartyController.onPageLoad(CheckMode).url
        )
          .withVisuallyHiddenText(messages("productsManagedByThirdParty.change.hidden"))
      )
    )
}
