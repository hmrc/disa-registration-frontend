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

import models.journeydata.thirdparty.{ThirdPartyOrganisations}
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.html.components.GovukSummaryList
import viewmodels.govuk.summarylist.*

import javax.inject.Inject

class ThirdPartiesCheckYourAnswersViewModel @Inject() (
                                                        govukSummaryList: GovukSummaryList
                                                      ) {

  def apply(section: ThirdPartyOrganisations)(implicit messages: Messages): Html = {

    // 🔹 1. TOP SECTION (no heading)
    val topSection: Seq[Html] =
      Seq(
        ProductsManagedByThirdPartySummary.row(section)
      ).flatten match {
        case rows if rows.nonEmpty =>
          Seq(govukSummaryList(SummaryListViewModel(rows)))
        case _ => Nil
      }

    // 🔹 2. THIRD PARTY SECTIONS
    val thirdPartySections =
      section.thirdParties.zipWithIndex.flatMap { case (tp, idx) =>
        if (tp.inProgress) Nil
        else {
          val displayIndex = idx + 1
          val rows =
            Seq(
              ThirdPartyOrgDetailsSummary.row(tp, displayIndex),
              ReturnsManagedByThirdPartySummary.row(tp),
              InvestorFundsUsedByThirdPartySummary.row(tp),
              ThirdPartyInvestorFundsPercentageSummary.row(tp)
            ).flatten

          if (rows.nonEmpty) {
            Seq(
              Html(
                s"""<h2 class="govuk-heading-m">${messages("thirdPartiesCheckYourAnswers.sub.heading", displayIndex)}</h2>"""
              ),
              govukSummaryList(SummaryListViewModel(rows))
            )
          } else Nil
        }
      }

    // 🔹 3. GLOBAL SECTIONS (with headings)
    val globalSections =
      Seq(
        ConnectedOrganisationsSummary.row(section).map { row =>
          HtmlFormat.fill(
            Seq(
              Html(
                s"""<h2 class="govuk-heading-m">${messages("connectedOrganisations.heading")}</h2>"""
              ),
              govukSummaryList(SummaryListViewModel(Seq(row)))
            )
          )
        },
          HtmlFormat.fill(
            Seq(
              Html(
                s"""<h2 class="govuk-heading-m">${messages("addAnother.heading")}</h2>"""
              ),
              govukSummaryList(SummaryListViewModel(Seq(AddAnotherThirdPartySummary.row(section))))
            )
          )
      ).flatten

    HtmlFormat.fill(
      topSection ++ thirdPartySections ++ globalSections
    )
  }
}