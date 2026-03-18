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

import forms.OrganisationTelephoneNumberFormProvider.digitsOnlyPattern
import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

import scala.collection.immutable.ArraySeq

class OrganisationTelephoneNumberFormProviderSpec extends StringFieldBehaviours {

  val requiredKey          = "organisationTelephoneNumber.error.required"
  val tooLongKey           = "organisationTelephoneNumber.error.tooLong"
  val tooShortKey          = "organisationTelephoneNumber.error.tooShort"
  val invalidKey           = "organisationTelephoneNumber.error.invalid"
  val maxLength, minLength = 11

  val form = new OrganisationTelephoneNumberFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      numericOfLength(minLength, maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, tooLongKey, Seq(maxLength))
    )

    behave like fieldWithMinLength(
      form,
      fieldName,
      minLength = minLength,
      lengthError = FormError(fieldName, tooShortKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    behave like fieldWithPattern(
      form,
      fieldName,
      pattern = digitsOnlyPattern.r,
      error = FormError(fieldName, invalidKey, ArraySeq(digitsOnlyPattern))
    )
  }
}
