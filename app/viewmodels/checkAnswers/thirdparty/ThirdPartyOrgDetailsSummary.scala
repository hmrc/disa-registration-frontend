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

import controllers.thirdparty.routes.ThirdPartyOrgDetailsController
import models.CheckMode
import models.journeydata.thirdparty.ThirdParty
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object ThirdPartyOrgDetailsSummary {

  def row(thirdParty: ThirdParty, index: Int)(implicit messages: Messages): Option[SummaryListRow] =
    thirdParty.thirdPartyName.map { name =>

      val content =
        thirdParty.thirdPartyFrn match {
          case Some(frn) =>
            HtmlFormat.fill(
              Seq(
                HtmlFormat.escape(name),
                Html("<br>"),
                HtmlFormat.escape(frn)
              )
            )
          case None      =>
            HtmlFormat.escape(name)
        }

      SummaryListRowViewModel(
        key = Key(Text(messages("thirdPartyOrgDetails.checkYourAnswersLabel", index))),
        value = ValueViewModel(HtmlContent(content)),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            ThirdPartyOrgDetailsController.onPageLoad(Some(thirdParty.id), CheckMode).url
          ).withVisuallyHiddenText(messages("thirdPartyOrgDetails.change.hidden", index))
        )
      )
    }
}
