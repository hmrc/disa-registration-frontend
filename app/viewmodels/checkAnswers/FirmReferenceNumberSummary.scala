/*
 * Copyright 2025 HM Revenue & Customs
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

import controllers.orgdetails.routes.FirmReferenceNumberController
import models.CheckMode
import models.journeydata.JourneyData
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object FirmReferenceNumberSummary {

  def row(answers: JourneyData)(implicit messages: Messages): Option[SummaryListRow] =
    answers.organisationDetails.flatMap(_.fcaNumber).map { answer =>
      SummaryListRowViewModel(
        key = "firmReferenceNumber.checkYourAnswersLabel",
        value = ValueViewModel(HtmlFormat.escape(answer).toString),
        actions = Seq(
          ActionItemViewModel("site.change", FirmReferenceNumberController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("firmReferenceNumber.change.hidden"))
        )
      )
    }
}
