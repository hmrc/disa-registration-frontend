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

package views

import forms.PeerToPeerPlatformNumberFormProvider
import models.NormalMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import views.html.isaProducts.PeerToPeerPlatformNumberView

class PeerToPeerPlatformNumberViewSpec extends ViewSpecBase {

  private val form: Form[String] = new PeerToPeerPlatformNumberFormProvider().apply(testString)
  private val view               = app.injector.instanceOf[PeerToPeerPlatformNumberView]

  private def formInvalid(value: String): Document = {
    val invalid = form.bind(Map("value" -> value))
    Jsoup.parse(view(invalid, testString, NormalMode).body)
  }

  "PeerToPeerPlatformNumberView" should {

    "render input requirement error properly" in {
      val doc = formInvalid("")

      val errorSummary = doc.select(".govuk-error-summary__list a").text()
      errorSummary must include(messages("peerToPeerPlatformNumber.error.required", testString))
    }

    "render pattern requirement error properly" in {
      val doc = formInvalid("aaa")

      val errorSummary = doc.select(".govuk-error-summary__list a").text()
      errorSummary must include(messages("peerToPeerPlatformNumber.error.pattern", testString))
    }
  }
}
