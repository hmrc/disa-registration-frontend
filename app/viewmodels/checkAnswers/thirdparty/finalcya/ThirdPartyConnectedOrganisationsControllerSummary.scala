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

package viewmodels.checkAnswers.thirdparty.finalcya

import controllers.thirdparty.routes.*
import models.{CheckMode, ReturnTo}
import models.journeydata.thirdparty.ThirdPartyOrganisations
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow, Value}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object ThirdPartyConnectedOrganisationsControllerSummary {

  def row(thirdPartyOrganisations: ThirdPartyOrganisations, returnTo: Option[ReturnTo] = None)(implicit
    messages: Messages
  ): Option[SummaryListRow] = {

    val connectedOrgs = thirdPartyOrganisations.connectedOrganisations

    val displayValue: Value =
      if (connectedOrgs.nonEmpty) {
        ValueViewModel(
          HtmlContent(
            Html(
              connectedOrgs
                .map(answer => HtmlFormat.escape(answer).body)
                .mkString(",<br>")
            )
          )
        )
      } else {
        ValueViewModel(Text(messages("site.no")))
      }

    Some(
      SummaryListRowViewModel(
        key = Key(Text(messages("thirdPartyConnectedOrganisations.checkYourAnswersLabel"))),
        value = displayValue,
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            ThirdPartyConnectedOrganisationsController
              .onPageLoad(CheckMode, returnTo)
              .url
          ).withVisuallyHiddenText(
            messages("connectedOrganisations.change.hidden")
          )
        )
      )
    )
  }
}
