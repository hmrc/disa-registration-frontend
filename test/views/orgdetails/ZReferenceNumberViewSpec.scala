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

package views.orgdetails

import forms.ZReferenceNumberFormProvider
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import views.ViewSpecBase
import views.html.orgdetails.ZReferenceNumberView

class ZReferenceNumberViewSpec extends ViewSpecBase {

  private val form: Form[String] = new ZReferenceNumberFormProvider().apply()
  private val view               = app.injector.instanceOf[ZReferenceNumberView]

  private def docEmpty: Document = Jsoup.parse(view(form).body)

  private def docWith(value: String): Document = {
    val bound = form.bind(Map("value" -> value))
    Jsoup.parse(view(bound).body)
  }

  private def docInvalid(value: String): Document = {
    val invalid = form.bind(Map("value" -> value))
    Jsoup.parse(view(invalid).body)
  }

  "ZReferenceNumberView" should {

    "render the page heading and caption" in {
      val doc = docEmpty

      val caption = doc.select("span[class*=govuk-caption]").text
      caption must include("Organisation details")

      val h1 = doc.select("h1 > label").get(0).ownText
      h1 mustBe "What is your Z-reference number?"
    }

    "render the form posting to Z ref page with a single text input named 'value'" in {
      val doc = docEmpty

      val formEl = doc.selectFirst("form")
      Option(formEl) mustBe defined
      formEl.attr("action") mustBe controllers.orgdetails.routes.ZReferenceNumberController.onSubmit().url
      formEl.attr("method").toLowerCase mustBe "post"

      val input = doc.selectFirst("input[name=value]")
      Option(input) mustBe defined
    }

    "render the hint text under the input" in {
      val doc  = docEmpty
      val hint = doc.select(".govuk-hint").text()

      hint must include("This is usually a Z followed by 4 numbers - for example, Z1234")
    }

    "render a primary submit button" in {
      val doc = docEmpty
      val btn = doc.selectFirst(".govuk-button")

      Option(btn) mustBe defined
      btn.text() must include("Save and continue")
    }

    "preserve the entered value when bound successfully" in {
      val doc   = docWith("Z1234")
      val input = doc.selectFirst("input[name=value]")

      Option(input) mustBe defined
      input.attr("value") mustBe "Z1234"
    }

    "show an error summary and inline error when value is missing" in {
      val doc = docInvalid("")

      val summary = doc.selectFirst(".govuk-error-summary")

      Option(summary) mustBe defined
      summary.text() must include("There is a problem")
      summary.text() must include("You need to tell us what your Z reference number is")

      val input = doc.selectFirst("input[name=value]")

      Option(input) mustBe defined
      input.classNames() must contain("govuk-input--error")

      val firstLink = summary.selectFirst("a[href^=#]")

      Option(firstLink) mustBe defined
      firstLink.attr("href") must be("#value")
    }

    "show the correct error message when value is in the wrong format" in {
      val doc = docInvalid("bad")

      val summary = doc.selectFirst(".govuk-error-summary").select("a")

      Option(summary) mustBe defined
      summary.text() mustBe "Enter the Z-reference number in the correct format. It's usually a Z followed by 4 numbers"
    }
  }
}
