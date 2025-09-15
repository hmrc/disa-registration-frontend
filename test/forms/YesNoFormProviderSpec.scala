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

package forms

import play.api.data.{Form, FormError}

class YesNoFormProviderSpec extends FormSpec {

  private val form: Form[Boolean] = new YesNoFormProvider()("error")
  private val fieldKey            = "value"

  "YesNoFormProvider" - {

    "bind a valid response" in {
      val result = form.bind(Map(fieldKey -> "true"))
      result.errors mustBe Nil
      result.value.get mustBe true
    }

    "fail when missing" in {
      checkForError(form, Map(fieldKey -> ""), Seq(FormError(fieldKey, "error")))
    }
  }
}
