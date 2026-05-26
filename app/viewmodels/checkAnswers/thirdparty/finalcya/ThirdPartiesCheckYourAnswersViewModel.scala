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

import models.ReturnTo.MultipleThirdPartiesCya
import models.journeydata.thirdparty.ThirdPartyOrganisations
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.html.components.GovukSummaryList
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*

import javax.inject.Inject

class ThirdPartiesCheckYourAnswersViewModel @Inject() (
  govukSummaryList: GovukSummaryList
) {

  def apply(section: ThirdPartyOrganisations)(implicit messages: Messages): Html =
    HtmlFormat.fill(
      buildTopSection(section) ++
        buildThirdPartySections(section) ++
        buildGlobalSections(section)
    )

  private def buildTopSection(section: ThirdPartyOrganisations)(implicit messages: Messages): Seq[Html] = {
    val rows =
      Seq(
        ProductsManagedByThirdPartySummary.row(section)
      ).flatten

    if (rows.nonEmpty)
      Seq(govukSummaryList(SummaryListViewModel(rows)))
    else
      Nil
  }

  private def buildThirdPartySections(section: ThirdPartyOrganisations)(implicit messages: Messages): Seq[Html] =
    section.thirdParties.zipWithIndex.flatMap { case (tp, idx) =>
      if (tp.inProgress) {
        Nil
      } else {
        val displayIndex = idx + 1
        val rows         = buildThirdPartyRows(tp, displayIndex)

        if (rows.isEmpty) Nil
        else
          Seq(
            sectionHeading("thirdPartiesCheckYourAnswers.sub.heading", displayIndex),
            govukSummaryList(SummaryListViewModel(rows))
          )
      }
    }

  private def buildThirdPartyRows(tp: models.journeydata.thirdparty.ThirdParty, index: Int)(implicit
    messages: Messages
  ): Seq[SummaryListRow] =
    Seq(
      ThirdPartyOrgDetailsSummary.row(tp, index, Some(MultipleThirdPartiesCya)),
      ThirdPartyManagingReturnsSummary.row(tp, Some(MultipleThirdPartiesCya)),
      InvestorFundsUsedByThirdPartySummary.row(tp, Some(MultipleThirdPartiesCya)),
      ThirdPartyInvestorFundsPercentageSummary.row(tp, Some(MultipleThirdPartiesCya))
    ).flatten

  private def buildGlobalSections(section: ThirdPartyOrganisations)(implicit messages: Messages): Seq[Html] =
    Seq(
      sectionHeading("thirdPartiesCheckYourAnswers.connectedOrganisations.sub.heading"),
      govukSummaryList(
        SummaryListViewModel(
          ThirdPartyConnectedOrganisationsControllerSummary.row(section).toSeq
        )
      ),
      sectionHeading("thirdPartiesCheckYourAnswers.addAnother.sub.heading"),
      govukSummaryList(
        SummaryListViewModel(
          Seq(AddAnotherThirdPartySummary.row(section))
        )
      )
    )

  private def sectionHeading(messageKey: String, args: Any*)(implicit messages: Messages): Html =
    Html(
      s"""<h2 class="govuk-heading-m">
         |${messages(messageKey, args: _*)}
         |</h2>""".stripMargin
    )
}
