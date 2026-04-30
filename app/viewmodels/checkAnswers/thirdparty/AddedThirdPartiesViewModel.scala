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

import config.FrontendAppConfig
import controllers.routes.*
import controllers.thirdparty.routes.*
import models.journeydata.thirdparty.ThirdParty
import models.{NormalMode, YesNoAnswer}
import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.html.components.{GovukRadios, GovukSummaryList}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.all.{FluentLegend, LegendViewModel, RadiosViewModel}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

import javax.inject.Inject

case class AddedThirdPartiesSummary(
  inProgress: Seq[ThirdParty],
  complete: Seq[ThirdParty],
  maxThirdParties: Int
) {
  def count: Int =
    inProgress.size + complete.size

  def multiple: Boolean =
    count > 1

  def titleKey: String =
    if (multiple) "addedThirdParties.title.plural"
    else "addedThirdParties.title"

  def headingKey: String =
    if (multiple) "addedThirdParties.heading.plural"
    else "addedThirdParties.heading"

  def title(implicit messages: Messages): String =
    messages(titleKey, count)

  def heading(implicit messages: Messages): String =
    messages(headingKey, count)

}

class AddedThirdPartiesViewModel @Inject() (
  govukSummaryList: GovukSummaryList,
  govukRadios: GovukRadios,
  appConfig: FrontendAppConfig
) {

  def apply(
    form: Form[_],
    inProgress: Seq[ThirdParty],
    complete: Seq[ThirdParty]
  )(implicit messages: Messages): Html = {

    val count = inProgress.size + complete.size

    HtmlFormat.fill(
      Seq(
        Option.when(complete.nonEmpty) {
          HtmlFormat.fill(
            Seq(
              Option
                .when(inProgress.nonEmpty) {
                  Html(
                    s"""<h2 class="govuk-heading-m">${HtmlFormat.escape(messages("addedThirdParties.complete"))}</h2>"""
                  )
                }
                .getOrElse(HtmlFormat.empty),
              govukSummaryList(SummaryListViewModel(rows = complete.flatMap(thirdParty => row(thirdParty))))
            )
          )
        },
        Option.when(inProgress.nonEmpty) {
          HtmlFormat.fill(
            Seq(
              Html(
                s"""<h2 class="govuk-heading-m">${HtmlFormat.escape(messages("addedThirdParties.inProgress"))}</h2>"""
              ),
              govukSummaryList(
                SummaryListViewModel(rows = inProgress.flatMap(thirdParty => row(thirdParty, inProgress = true)))
              )
            )
          )
        },
        Option.when(count < appConfig.maxThirdParties) {
          govukRadios(
            RadiosViewModel(
              field = form("value"),
              legend = LegendViewModel(messages("addedThirdParties.addAnother"))
                .withCssClass("govuk-fieldset__legend--m"),
              items = YesNoAnswer.options
            )
          )
        }
      ).flatten
    )
  }

  private def row(thirdParty: ThirdParty, inProgress: Boolean = false)(implicit
    messages: Messages
  ): Option[SummaryListRow] = {
    val changeLink = if (inProgress) {
      ThirdPartyOrgDetailsController
        .onPageLoad(Some(thirdParty.id), NormalMode)
        .url
    } else {
      ThirdPartyCheckYourAnswersController.onPageLoad(thirdParty.id).url
    }
    thirdParty.thirdPartyName.map { name =>
      SummaryListRowViewModel(
        key = KeyViewModel(HtmlFormat.escape(name).toString)
          .withCssClass("govuk-!-font-weight-regular"),
        value = ValueViewModel("")
          .withCssClass("govuk-!-width-one-quarter"),
        actions = Seq(
          ActionItemViewModel(
            content = messages("site.change"),
            href = changeLink
          ).withVisuallyHiddenText(
            messages("addedThirdParties.summary.action.hidden", name)
          ),
          ActionItemViewModel(
            content = messages("site.remove"),
            href = TaskListController.onPageLoad().url
          ).withVisuallyHiddenText(
            messages("addedThirdParties.summary.action.hidden", name)
          )
        )
      )
    }
  }
}
