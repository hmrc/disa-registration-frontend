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

package views.components

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Field, Form}
import play.api.data.Forms.{boolean, single}
import views.ViewSpecBase
import views.html.components.InputYesNo

class InputYesNoSpec extends ViewSpecBase {

  private val form = Form(single("v" -> boolean))
  private val view = app.injector.instanceOf[InputYesNo]

  private def fieldOkTrue  = form.bind(Map("v" -> "true"))("v")
  private def fieldOkFalse = form.bind(Map("v" -> "false"))("v")

  private def renderDoc(
    field: Field,
    classes: String = "govuk-radios--inline",
    hintKey: Option[String] = None,
    captionKey: Option[String] = None,
    legendKey: String = "legend.key",
    legendClasses: String = "govuk-fieldset__legend--m",
    legendIsPageHeading: Boolean = false
  ): Document = {
    val html = view(
      field = field,
      classes = classes,
      hintKey = hintKey,
      captionKey = captionKey,
      legendKey = legendKey,
      legendClasses = legendClasses,
      legendIsPageHeading = legendIsPageHeading
    )
    Jsoup.parse(html.body)
  }

  "InputYesNo component" should {

    "render an inline yes/no radios group with legend and hint, no caption" in {
      val doc = renderDoc(fieldOkTrue, hintKey = Some("hint.key"))

      val legend = Option(doc.selectFirst("legend.govuk-fieldset__legend.govuk-fieldset__legend--m"))
      legend mustBe defined
      legend.get.text() must include(messages("legend.key"))

      val radios = Option(doc.selectFirst("div.govuk-radios"))
      radios mustBe defined
      radios.get.classNames() must contain("govuk-radios--inline")

      val yes = Option(doc.selectFirst("input#v[type=radio][name=v]"))
      yes mustBe defined
      yes.get.attr("value") mustBe "true"

      val no = Option(doc.selectFirst("input#v-no[type=radio][name=v]"))
      no mustBe defined
      no.get.attr("value") mustBe "false"

      val labelsText = doc.select("label.govuk-radios__label").eachText()
      labelsText must contain(messages("site.yes"))
      labelsText must contain(messages("site.no"))

      val hint = Option(doc.selectFirst(".govuk-hint"))
      hint mustBe defined
      hint.get.text() mustBe messages("hint.key")

      Option(doc.selectFirst(".hmrc-caption")) mustBe empty
      Option(doc.selectFirst("h1")) mustBe empty
    }

    "render page heading and caption when legend is page heading and caption is present" in {
      val doc = renderDoc(
        fieldOkFalse,
        captionKey = Some("caption.key"),
        legendIsPageHeading = true
      )

      val h1 = Option(doc.selectFirst("h1"))
      h1 mustBe defined
      h1.get.text() must include(messages("legend.key"))

      val caption = Option(doc.selectFirst(".hmrc-caption"))
      caption mustBe defined
      caption.get.ownText() mustBe messages("caption.key")
    }

    "apply custom radios classes and custom legend classes" in {
      val doc = renderDoc(
        fieldOkTrue,
        classes = "custom another",
        legendClasses = "govuk-fieldset__legend--l"
      )

      val radios = Option(doc.selectFirst("div.govuk-radios"))
      radios mustBe defined
      radios.get.classNames() must contain allOf ("custom", "another")

      val legend = Option(doc.selectFirst("legend.govuk-fieldset__legend.govuk-fieldset__legend--l"))
      legend mustBe defined
      legend.get.text() must include(messages("legend.key"))
    }
  }
}
