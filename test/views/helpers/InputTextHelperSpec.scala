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

package views.helpers

import play.api.data.Form
import play.api.data.Forms.{nonEmptyText, single}
import views.ViewSpecBase

class InputTextHelperSpec extends ViewSpecBase {

  "InputTextHelper.fieldErrorMessages" should {
    "return None when no error" in {
      val form = Form(single("v" -> nonEmptyText)).bind(Map("v" -> "ok"))

      InputTextHelper.fieldErrorMessages(form("v"))(messages) mustBe None
    }
    "return Some(ErrorMessage) when there is an error" in {
      val form   = Form(single("v" -> nonEmptyText)).bind(Map("v" -> ""))
      val result = InputTextHelper.fieldErrorMessages(form("v"))(messages)

      result.isDefined mustBe true
      result.get.content.asHtml.body must include("This field is required")
    }
  }
}
