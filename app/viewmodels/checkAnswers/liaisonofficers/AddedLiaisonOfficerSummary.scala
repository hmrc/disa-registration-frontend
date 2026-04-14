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

import config.FrontendAppConfig
import controllers.liaisonofficers.routes.{LiaisonOfficerNameController, RemoveLiaisonOfficerController}
import models.journeydata.liaisonofficers.LiaisonOfficer
import models.{CheckMode, YesNoAnswer}
import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.html.components.{GovukRadios, GovukSummaryList}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.all.{FluentLegend, LegendViewModel, RadiosViewModel}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

import javax.inject.Inject

case class AddedLiaisonOfficerSummary(
  inProgress: Seq[LiaisonOfficer],
  complete: Seq[LiaisonOfficer]
)

class AddedLiaisonOfficersViewModel @Inject() (
  govukSummaryList: GovukSummaryList,
  govukRadios: GovukRadios,
  appConfig: FrontendAppConfig
) {

  def apply(
    form: Form[_],
    inProgress: Seq[LiaisonOfficer],
    complete: Seq[LiaisonOfficer]
  )(implicit messages: Messages): Html = {

    val count = inProgress.size + complete.size

    HtmlFormat.fill(
      Seq(
        Option.when(complete.nonEmpty) {
          HtmlFormat.fill(
            Seq(
              Option
                .when(inProgress.nonEmpty) {
                  Html(s"""<h2 class="govuk-heading-m">${HtmlFormat.escape(
                      messages("addedLiaisonOfficers.complete")
                    )}</h2>""")
                }
                .getOrElse(HtmlFormat.empty),
              govukSummaryList(SummaryListViewModel(rows = complete.flatMap(row)))
            )
          )
        },
        Option.when(inProgress.nonEmpty) {
          HtmlFormat.fill(
            Seq(
              Html(s"""<h2 class="govuk-heading-m">${HtmlFormat.escape(
                  messages("addedLiaisonOfficers.inProgress")
                )}</h2>"""),
              govukSummaryList(SummaryListViewModel(rows = inProgress.flatMap(row)))
            )
          )
        },
        Option.when(count < appConfig.maxLos) {
          govukRadios(
            RadiosViewModel(
              field = form("value"),
              legend = LegendViewModel(messages("addedLiaisonOfficers.legend"))
                .withCssClass("govuk-fieldset__legend--m"),
              items = YesNoAnswer.options
            )
          )
        }
      ).flatten
    )
  }

  private def row(liaisonOfficer: LiaisonOfficer)(implicit messages: Messages): Option[SummaryListRow] =
    liaisonOfficer.fullName.map { answer =>
      SummaryListRowViewModel(
        key = KeyViewModel(HtmlFormat.escape(answer).toString)
          .withCssClass("govuk-!-font-weight-regular"),
        value = ValueViewModel("")
          .withCssClass("govuk-!-width-one-quarter"),
        actions = Seq(
          ActionItemViewModel(
            content = messages("site.change"),
            href = LiaisonOfficerNameController.onPageLoad(Some(liaisonOfficer.id), CheckMode).url
          ).withVisuallyHiddenText(messages("addedLiaisonOfficers.summary.action.hidden")),
          ActionItemViewModel(
            content = messages("site.remove"),
            href = RemoveLiaisonOfficerController.onPageLoad(liaisonOfficer.id).url
          ).withVisuallyHiddenText(messages("addedLiaisonOfficers.summary.action.hidden"))
        )
      )
    }
}
