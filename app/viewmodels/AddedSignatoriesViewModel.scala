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

package viewmodels

import config.FrontendAppConfig
import controllers.signatories.routes.{RemoveSignatoryController, SignatoryNameController}
import models.journeydata.signatories.Signatory
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

case class AddedSignatoriesSummary(
                                    inProgress: Seq[Signatory],
                                    complete: Seq[Signatory],
                                    maxSignatories: Int
                                  ) {
  def count: Int =
    inProgress.size + complete.size

  def multiple: Boolean =
    count > 1

  def titleKey: String =
    if (multiple) "addedSignatory.title.plural"
    else "addedSignatory.title"

  def headingKey: String =
    if (multiple) "addedSignatory.heading.plural"
    else "addedSignatory.heading"

  def title(implicit messages: Messages): String =
      messages(titleKey, count)

  def heading(implicit messages: Messages): String =
    messages(headingKey, count)

}



class AddedSignatoriesViewModel @Inject()(
                                           govukSummaryList: GovukSummaryList,
                                           govukRadios: GovukRadios,
                                           appConfig: FrontendAppConfig
                                         ) {

  def apply(
             form: Form[_],
             inProgress: Seq[Signatory],
             complete: Seq[Signatory]
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
                    s"""<h2 class="govuk-heading-m">${
                      HtmlFormat.escape(messages("addedSignatory.complete"))
                    }</h2>""")
                }
                .getOrElse(HtmlFormat.empty),

              govukSummaryList(
                SummaryListViewModel(rows = complete.flatMap(row))
              )
            )
          )
        },

        Option.when(inProgress.nonEmpty) {
          HtmlFormat.fill(
            Seq(
              Html(
                s"""<h2 class="govuk-heading-m">${
                  HtmlFormat.escape(messages("addedSignatory.inProgress"))
                }</h2>"""),

              govukSummaryList(
                SummaryListViewModel(rows = inProgress.flatMap(row))
              )
            )
          )
        },
        Option.when(count < appConfig.maxSignatories) {
          govukRadios(
            RadiosViewModel(
              field = form("value"),
              legend = LegendViewModel(messages("addedSignatory.addAnother"))
                .withCssClass("govuk-fieldset__legend--m"),
              items = YesNoAnswer.options
            )
          )
        }
      ).flatten
    )
  }

  private def row(signatory: Signatory)(implicit messages: Messages): Option[SummaryListRow] =
    signatory.fullName.map { name =>
      SummaryListRowViewModel(
        key = KeyViewModel(HtmlFormat.escape(name).toString)
          .withCssClass("govuk-!-font-weight-regular"),
        value = ValueViewModel("")
          .withCssClass("govuk-!-width-one-quarter"),
        actions = Seq(
          ActionItemViewModel(
            content = messages("site.change"),
            href = SignatoryNameController
              .onPageLoad(Some(signatory.id), CheckMode)
              .url
          ).withVisuallyHiddenText(
            messages("addedSignatory.summary.action.hidden", name)
          ),

          ActionItemViewModel(
            content = messages("site.remove"),
            href = RemoveSignatoryController
              .onPageLoad(signatory.id)
              .url
          ).withVisuallyHiddenText(
            messages("addedSignatory.summary.action.hidden", name)
          )
        )
      )
    }
}