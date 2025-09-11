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

final class ZReferenceNumberFormProviderSpec extends FormSpec {

  private val form: Form[String] = new ZReferenceNumberFormProvider()()
  private val fieldKey           = "value"

  "ZReferenceNumberFormProvider" - {

    "bind a valid Z-ref like Z1234" in {
      val result = form.bind(Map(fieldKey -> "Z1234"))
      result.errors mustBe Nil
      result.value.get mustBe "Z1234"
    }

    "bind a valid Z-ref with whitespace" in {
      val result = form.bind(Map(fieldKey -> "   Z1234   "))
      result.errors mustBe Nil
      result.value.get mustBe "Z1234"
    }

    "fail when missing" in {
      checkForError(form, Map(fieldKey -> ""), Seq(FormError(fieldKey, "orgDetails.zReferenceNumber.error.missing")))
    }

    "fail for lowercase z" in {
      checkForError(
        form,
        Map(fieldKey -> "z1234"),
        Seq(FormError(fieldKey, "orgDetails.zReferenceNumber.error.invalid", Seq("^Z[0-9]{4}$")))
      )
    }

    "fail for wrong length" in {
      checkForError(
        form,
        Map(fieldKey -> "Z12"),
        Seq(FormError(fieldKey, "orgDetails.zReferenceNumber.error.invalid", Seq("^Z[0-9]{4}$")))
      )
      checkForError(
        form,
        Map(fieldKey -> "Z12345"),
        Seq(FormError(fieldKey, "orgDetails.zReferenceNumber.error.invalid", Seq("^Z[0-9]{4}$")))
      )
    }

    "fail for wrong prefix" in {
      checkForError(
        form,
        Map(fieldKey -> "A1234"),
        Seq(FormError(fieldKey, "orgDetails.zReferenceNumber.error.invalid", Seq("^Z[0-9]{4}$")))
      )
    }
  }
}
