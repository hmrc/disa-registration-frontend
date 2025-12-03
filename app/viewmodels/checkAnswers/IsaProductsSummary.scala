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

import controllers.{IsaProductsController, routes}
import models.CheckMode
import models.journeyData.JourneyData
import pages.IsaProductsPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object IsaProductsSummary {

  def row(answers: JourneyData)(implicit messages: Messages): Option[SummaryListRow] =
    answers.isaProducts.map(_.dataItem2).map { answers =>
      val value = ValueViewModel(
        HtmlContent(
          answers
            .map { answer =>
              HtmlFormat.escape(messages(s"isaProducts.$answer")).toString
            }
            .mkString(",<br>")
        )
      )

      SummaryListRowViewModel(
        key = "isaProducts.checkYourAnswersLabel",
        value = value,
        actions = Seq(
          ActionItemViewModel("site.change", routes.IsaProductsController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("isaProducts.change.hidden"))
        )
      )
    }
}
