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

import play.api.data.Form
import play.api.data.Forms.{nonEmptyText, single}
import views.ViewSpecBase
import views.html.components.InputText

class InputTextSpec extends ViewSpecBase {

  private val form         = Form(single("v" -> nonEmptyText))
  private def fieldNoErr   = form.bind(Map("v" -> "abc"))("v")
  private def fieldWithErr = form.bind(Map("v" -> ""))("v")

  "InputText component" should {

    "render an input without error" in {
      val html = app.injector
        .instanceOf[InputText]
        .apply(
          field = fieldNoErr,
          labelKey = Some("label.key"),
          hintKey = Some("hint here"),
          labelIsPageHeading = false
        )

      html.body must include("name=\"v\"")
      html.body must include("hint here")
    }

    "render an input with error message" in {
      val html = app.injector
        .instanceOf[InputText]
        .apply(
          field = fieldWithErr,
          labelKey = Some("label.key")
        )

      html.body must include("govuk-input--error")
    }
  }
}
