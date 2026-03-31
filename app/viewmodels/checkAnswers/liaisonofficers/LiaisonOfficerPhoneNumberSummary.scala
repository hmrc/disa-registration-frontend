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

import controllers.liaisonofficers.routes.LiaisonOfficerPhoneNumberController
import models.CheckMode
import models.journeydata.liaisonofficers.LiaisonOfficer
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object LiaisonOfficerPhoneNumberSummary {

  def row(liaisonOfficer: LiaisonOfficer)(implicit messages: Messages): Option[SummaryListRow] =
    liaisonOfficer.phoneNumber.map { answer =>
      SummaryListRowViewModel(
        key = "liaisonOfficerPhoneNumber.checkYourAnswersLabel",
        value = ValueViewModel(HtmlFormat.escape(answer).toString),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            LiaisonOfficerPhoneNumberController.onPageLoad(liaisonOfficer.id, CheckMode).url
          )
            .withVisuallyHiddenText(messages("liaisonOfficerPhoneNumber.change.hidden"))
        )
      )
    }
}
