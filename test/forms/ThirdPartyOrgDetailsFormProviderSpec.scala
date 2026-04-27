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

import forms.ThirdPartyOrgDetailsFormProvider.{frnRegex, thirdPartyNameRegex}
import forms.behaviours.StringFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError

class ThirdPartyOrgDetailsFormProviderSpec extends StringFieldBehaviours {

  val form = new ThirdPartyOrgDetailsFormProvider()()

  val nameRequiredKey = "thirdPartyOrgDetails.thirdPartyName.error.required"
  val nameInvalidKey  = "thirdPartyOrgDetails.thirdPartyName.error.invalid"

  val frnLengthKey  = "thirdPartyOrgDetails.frn.error.incorrectLength"
  val frnPatternKey = "thirdPartyOrgDetails.frn.error.invalid"

  val nameField = "thirdPartyName"
  val frnField  = "frn"

  val unicodeLetter: Gen[Char] =
    Gen
      .choose(Char.MinValue, Char.MaxValue)
      .suchThat(Character.isLetter)

  val validChar: Gen[Char] =
    Gen.frequency(
      20 -> unicodeLetter,
      1  -> Gen.const(' '),
      1  -> Gen.const('-'),
      1  -> Gen.const('\''),
      1  -> Gen.const('’')
    )

  val validName: Gen[String] =
    Gen.choose(1, 35).flatMap { n =>
      Gen.listOfN(n, validChar).map(_.mkString).suchThat(_.trim.nonEmpty)
    }

  ".thirdPartyName" - {

    behave like fieldThatBindsValidData(
      form,
      nameField,
      validName
    )

    behave like fieldWithPattern(
      form,
      nameField,
      thirdPartyNameRegex.r,
      error = FormError(nameField, nameInvalidKey, Seq(thirdPartyNameRegex))
    )

    behave like mandatoryField(
      form,
      nameField,
      requiredError = FormError(nameField, nameRequiredKey)
    )
  }

  ".frn" - {

    behave like fieldThatBindsValidData(
      form,
      frnField,
      numericOfLength(6, 7)
    )

    behave like fieldWithMinLength(
      form,
      frnField,
      minLength = 6,
      lengthError = FormError(frnField, frnLengthKey, Seq(6))
    )

    behave like fieldWithMaxLength(
      form,
      frnField,
      maxLength = 7,
      lengthError = FormError(frnField, frnLengthKey, Seq(7))
    )

    behave like fieldWithPattern(
      form,
      frnField,
      pattern = frnRegex.r,
      error = FormError(frnField, frnPatternKey, Seq(frnRegex))
    )
  }
}
