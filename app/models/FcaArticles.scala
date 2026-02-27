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

package models

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.CheckboxItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import viewmodels.govuk.checkbox._

sealed trait FcaArticles

object FcaArticles extends Enumerable.Implicits {

  case object Article14 extends WithName("article14") with FcaArticles
  case object Article21 extends WithName("article21") with FcaArticles
  case object Article25 extends WithName("article25") with FcaArticles
  case object Article36H extends WithName("article36H") with FcaArticles
  case object Article37 extends WithName("article37") with FcaArticles
  case object Article39H extends WithName("article39H") with FcaArticles
  case object Article40 extends WithName("article40") with FcaArticles
  case object Article45 extends WithName("article45") with FcaArticles
  case object Article51ZA extends WithName("article51ZA") with FcaArticles
  case object Article51ZC extends WithName("article51ZC") with FcaArticles
  case object Article53 extends WithName("article53") with FcaArticles
  case object Article64 extends WithName("article64") with FcaArticles

  val values: Seq[FcaArticles] = Seq(
    Article14,
    Article21,
    Article25,
    Article36H,
    Article37,
    Article39H,
    Article40,
    Article45,
    Article51ZA,
    Article51ZC,
    Article53,
    Article64
  )

  def checkboxItems(implicit messages: Messages): Seq[CheckboxItem] =
    values.zipWithIndex.map {
      case (value, index) =>
        CheckboxItemViewModel(
          content = Text(messages(s"fcaArticles.${value.toString}")),
          fieldId = "value",
          index   = index,
          value   = value.toString
        )
    }

  implicit val enumerable: Enumerable[FcaArticles] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
