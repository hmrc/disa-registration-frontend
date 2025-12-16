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

package viewmodels.checkAnswers.isaproducts

import controllers.isaproducts.routes.InnovativeFinancialProductsController
import models.CheckMode
import models.journeydata.JourneyData
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object InnovativeFinancialProductsSummary {

  def row(answers: JourneyData)(implicit messages: Messages): Option[SummaryListRow] =
    answers.isaProducts.flatMap(_.innovativeFinancialProducts).map { answers =>
      val value = ValueViewModel(
        HtmlContent(
          answers
            .map { answer =>
              HtmlFormat.escape(messages(s"innovativeFinancialProducts.$answer")).toString
            }
            .mkString(",<br>")
        )
      )

      SummaryListRowViewModel(
        key = "innovativeFinancialProducts.checkYourAnswersLabel",
        value = value,
        actions = Seq(
          ActionItemViewModel("site.change", InnovativeFinancialProductsController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("innovativeFinancialProducts.change.hidden"))
        )
      )
    }
}
