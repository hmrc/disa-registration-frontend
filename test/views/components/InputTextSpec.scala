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
import play.api.data.Forms.{nonEmptyText, single}
import views.ViewSpecBase
import views.html.components.InputText

class InputTextSpec extends ViewSpecBase {

  private val form = Form(single("v" -> nonEmptyText))
  private val view = app.injector.instanceOf[InputText]

  private def fieldOk  = form.bind(Map("v" -> "Z1234"))("v")
  private def fieldErr = form.bind(Map("v" -> ""))("v")

  private def renderDoc(
    field: Field,
    inputClass: Option[String] = None,
    hintKey: Option[String] = None,
    autocomplete: Option[String] = None,
    captionKey: Option[String] = None,
    labelKey: Option[String] = None,
    labelIsPageHeading: Boolean = false
  ): Document = {
    val html = view(
      field = field,
      inputClass = inputClass,
      hintKey = hintKey,
      autocomplete = autocomplete,
      captionKey = captionKey,
      labelKey = labelKey,
      labelIsPageHeading = labelIsPageHeading
    )
    Jsoup.parse(html.body)
  }

  "InputText component" should {

    "render a standard labelled input with hint and no caption" in {
      val doc = renderDoc(fieldOk, hintKey = Some("hint.key"), labelKey = Some("label.key"))

      val label = Option(doc.selectFirst("label.govuk-label.govuk-label--m"))
      label mustBe defined
      label.get.text() mustBe messages("label.key")

      Option(doc.selectFirst("h1")) mustBe empty

      val input = Option(doc.selectFirst("input[name=v]"))
      input mustBe defined

      val hint = Option(doc.selectFirst(".govuk-hint"))
      hint mustBe defined
      hint.get.text() mustBe messages("hint.key")

      input.get.attr("aria-describedby") must not be empty
    }

    "render page heading and caption when label is page heading and caption is present" in {
      val doc =
        renderDoc(fieldOk, labelKey = Some("label.key"), captionKey = Some("caption.key"), labelIsPageHeading = true)

      val h1 = Option(doc.selectFirst(".govuk-label"))
      h1 mustBe defined
      h1.get.ownText mustBe messages("label.key")

      val caption = Option(doc.selectFirst(".hmrc-caption"))
      caption mustBe defined
      caption.get.ownText mustBe messages("caption.key")

      doc.select("label.govuk-label--m").size() mustBe 0
    }

    "apply custom classes and autocomplete" in {
      val doc = renderDoc(
        fieldOk,
        labelKey = Some("label.key"),
        inputClass = Some("custom another"),
        autocomplete = Some("postal-code")
      )

      val input = Option(doc.selectFirst("input[name=v]"))
      input mustBe defined
      input.get.classNames() must contain allOf ("custom", "another")
      input.get.attr("autocomplete") mustBe "postal-code"
    }

    "show error class and inline error message when the field has errors" in {
      val doc = renderDoc(fieldErr, labelKey = Some("label.key"))

      val input = Option(doc.selectFirst("input[name=v]"))
      input mustBe defined
      input.get.classNames() must contain("govuk-input--error")

      val inlineError = Option(doc.selectFirst(".govuk-error-message"))
      inlineError mustBe defined
      inlineError.get.text().toLowerCase must include(messages("error.required").toLowerCase)
    }

    "render without label when labelKey is None" in {
      val doc = renderDoc(fieldOk, labelKey = None)

      doc.select("label.govuk-label").size() mustBe 0
      doc.select("h1").size() mustBe 0
      Option(doc.selectFirst("input[name=v]")) mustBe defined
    }
  }
}
