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

import forms.EmailVerificationCodeFormProvider.emailVerificationCodePattern
import forms.behaviours.StringFieldBehaviours
import org.scalacheck.Gen.alphaChar
import play.api.data.FormError

class EmailVerificationCodeFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "emailVerificationCode.error.required"
  val formatKey   = "emailVerificationCode.error.format"
  val tooLongKey  = "emailVerificationCode.error.tooLong"
  val tooShortKey = "emailVerificationCode.error.tooShort"
  val maxLength   = 6
  val minLength   = 6

  val form = new EmailVerificationCodeFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      alphaOfLength(6, 6)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, tooLongKey, Seq(maxLength)),
      charType = alphaChar
    )

    behave like fieldWithMinLength(
      form,
      fieldName,
      minLength = minLength,
      lengthError = FormError(fieldName, tooShortKey, Seq(minLength)),
      charType = alphaChar
    )

    behave like fieldWithPattern(
      form,
      fieldName,
      "[A-Za-z]+".r,
      FormError(fieldName, formatKey, Seq(emailVerificationCodePattern))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
