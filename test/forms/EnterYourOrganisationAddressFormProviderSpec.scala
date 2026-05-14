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
import models.journeydata.CorrespondenceAddress
import org.scalacheck.Gen
import org.scalacheck.Gen.{alphaChar, alphaNumChar}
import play.api.data.FormError

class EnterYourOrganisationAddressFormProviderSpec extends StringFieldBehaviours {

  val form = new EnterYourOrganisationAddressFormProvider()()

  private val validData =
    Map(
      "addressLine1" -> "10 Downing Street",
      "addressLine2" -> "Westminster",
      "townOrCity"   -> "London",
      "postcode"     -> "SW1A 2AA"
    )

  ".addressLine1" - {

    val fieldName   = "addressLine1"
    val requiredKey = "enterYourOrganisationAddress.error.addressLine1.required"
    val lengthKey   = "enterYourOrganisationAddress.error.addressLine1.length"
    val maxLength   = 255

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }

  ".addressLine2" - {

    val fieldName = "addressLine2"
    val lengthKey = "enterYourOrganisationAddress.error.addressLine2.length"
    val maxLength = 255

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    "must bind when omitted" in {
      val result = form.bind(validData - fieldName)

      result.errors mustBe empty
      result.value.value.addressLine2 mustBe None
    }

    "must bind when empty" in {
      val result = form.bind(validData.updated(fieldName, ""))

      result.errors mustBe empty
      result.value.value.addressLine2 mustBe None
    }
  }

  ".townOrCity" - {

    val fieldName   = "townOrCity"
    val requiredKey = "enterYourOrganisationAddress.error.townOrCity.required"
    val lengthKey   = "enterYourOrganisationAddress.error.townOrCity.length"
    val maxLength   = 255

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }

  ".postcode" - {

    val fieldName   = "postcode"
    val requiredKey = "enterYourOrganisationAddress.error.postcode.required"
    val tooShortKey = "enterYourOrganisationAddress.error.postcode.tooShort"
    val tooLongKey  = "enterYourOrganisationAddress.error.postcode.tooLong"
    val invalidKey  = "enterYourOrganisationAddress.error.postcode.invalid"
    val minLength   = 5
    val maxLength   = 8

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      Gen.oneOf("SW1A 2AA", "B1 1AA", "BB11BB", "W1A 1HQ")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    behave like fieldWithMinLength(
      form,
      fieldName,
      minLength,
      FormError(fieldName, tooShortKey, Seq(minLength)),
      alphaNumChar
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength,
      FormError(fieldName, tooLongKey, Seq(maxLength)),
      alphaNumChar
    )

    "must fail to bind when the postcode format is invalid" in {
      val result = form.bind(validData.updated(fieldName, "ABCDE"))

      result.errors must contain only FormError(fieldName, invalidKey)
    }
  }

  "must bind valid data to a CorrespondenceAddress" in {
    val result = form.bind(validData)

    result.value.value mustBe CorrespondenceAddress(
      addressLine1 = Some("10 Downing Street"),
      addressLine2 = Some("Westminster"),
      addressLine3 = Some("London"),
      postCode = Some("SW1A 2AA")
    )
  }

  "must trim bound values" in {
    val result =
      form.bind(
        Map(
          "addressLine1" -> "  10 Downing Street  ",
          "addressLine2" -> "  Westminster  ",
          "townOrCity"   -> "  London  ",
          "postcode"     -> "  SW1A 2AA  "
        )
      )

    result.value.value mustBe CorrespondenceAddress(
      addressLine1 = Some("10 Downing Street"),
      addressLine2 = Some("Westminster"),
      addressLine3 = Some("London"),
      postCode = Some("SW1A 2AA")
    )
  }
}
