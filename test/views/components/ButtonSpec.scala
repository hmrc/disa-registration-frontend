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

import views.ViewSpecBase
import views.html.components.Button

class ButtonSpec extends ViewSpecBase {

  "Button component" should {
    "render a standard save-and-continue button" in {
      val html = app.injector.instanceOf[Button].apply()(messages)

      html.body must include(messages("site.saveAndContinue"))
      html.body must include("govuk-button")
    }

    "respect attributes and href" in {
      val html = app.injector
        .instanceOf[Button]
        .apply(
          messageKey = "site.start",
          isStartButton = true,
          attributes = Map("id" -> "submit"),
          href = Some("/start")
        )(messages)

      html.body must include(messages("site.start"))
      html.body must include("id=\"submit\"")
      html.body must include("href=\"/start\"")
    }
  }
}
