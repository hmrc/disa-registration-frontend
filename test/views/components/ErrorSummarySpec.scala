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

import play.api.data.FormError
import views.ViewSpecBase
import views.html.components.ErrorSummary

class ErrorSummarySpec extends ViewSpecBase {

  "ErrorSummary component" should {
    "render nothing when there are no errors" in {
      val html = app.injector.instanceOf[ErrorSummary].apply(Nil)

      html.body.trim mustBe ""
    }
    "render a GOV.UK error summary when errors exist" in {
      val errors = Seq(FormError("value", "form.error"))
      val html   = app.injector.instanceOf[ErrorSummary].apply(errors)

      html.body must include("govuk-error-summary")
      html.body must include("There is a problem")
      html.body must include("""<a href="#value">""")
    }
  }
}
