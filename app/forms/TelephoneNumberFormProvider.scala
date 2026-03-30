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

import forms.TelephoneNumberFormProvider.{digitsOnlyPattern, whitespacePattern}

import javax.inject.Inject
import forms.mappings.Mappings
import play.api.data.Form

class TelephoneNumberFormProvider @Inject() () extends Mappings {

  def apply(keyPrefix: String): Form[String] =
    Form(
      "value" -> text(s"$keyPrefix.error.required")
        .transform(formToModel => formToModel.replaceAll(whitespacePattern, ""), identity: String => String)
        .verifying(regexp(digitsOnlyPattern, s"$keyPrefix.error.invalid"))
        .verifying(minLength(11, s"$keyPrefix.error.tooShort"))
        .verifying(maxLength(11, s"$keyPrefix.error.tooLong"))
    )
}

object TelephoneNumberFormProvider {
  private[forms] val whitespacePattern = "\\s+"
  private[forms] val digitsOnlyPattern = "^\\d+$"
}
