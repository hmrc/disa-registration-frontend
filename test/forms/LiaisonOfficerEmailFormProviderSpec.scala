/*
 * Copyright 2026 HM Revenue & Customs
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

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError
import org.scalacheck.Gen

class LiaisonOfficerEmailFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "liaisonOfficerEmail.error.required"
  val invalidKey  = "liaisonOfficerEmail.error.invalid"

  val form = new LiaisonOfficerEmailFormProvider()()

  val emailRegex =
    "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$"

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      Gen.oneOf(
        "test@example.com",
        "user.name+tag@domain.co.uk",
        "user_name@sub.domain.com",
        "user-name@domain.org",
        "user%test@domain.io"
      )
    )

    behave like fieldWithPattern(
      form,
      fieldName,
      emailRegex.r,
      error = FormError(fieldName, invalidKey, Seq(emailRegex))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
