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

package viewmodels.checkAnswers.liaisonofficers

import controllers.liaisonofficers.routes.LiaisonOfficerCommunicationController
import models.CheckMode
import models.journeydata.liaisonofficers.LiaisonOfficer
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object LiaisonOfficerCommunicationSummary {

  def row(liaisonOfficer: LiaisonOfficer)(implicit messages: Messages): Option[SummaryListRow] =
    if (liaisonOfficer.communication.nonEmpty)
      val value = ValueViewModel(
        HtmlContent(
          liaisonOfficer.communication.zipWithIndex
            .map {
              case (s, 0) => HtmlFormat.escape(messages(s"liaisonOfficerCommunication.$s")).toString
              case (s, _) => HtmlFormat.escape(messages(s"liaisonOfficerCommunication.$s").toLowerCase).toString
            }
            .mkString(", ")
        )
      )

      Some(
        SummaryListRowViewModel(
          key = "liaisonOfficerCommunication.checkYourAnswersLabel",
          value = value,
          actions = Seq(
            ActionItemViewModel(
              "site.change",
              LiaisonOfficerCommunicationController.onPageLoad(liaisonOfficer.id, CheckMode).url
            )
              .withVisuallyHiddenText(messages("liaisonOfficerCommunication.change.hidden"))
          )
        )
      )
    else None
}
