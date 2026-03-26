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

import forms.LiaisonOfficerNameFormProvider.regexPattern
import forms.behaviours.StringFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError

class LiaisonOfficerNameFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "liaisonOfficerName.error.required"
  val invalidKey  = "liaisonOfficerName.error.invalid"

  val form = new LiaisonOfficerNameFormProvider()()

  def validNameString: Gen[String] = {
    val validChar = Gen.oneOf(
      ('a' to 'z') ++
        ('A' to 'Z') ++
        Seq(' ', '-', '\'')
    )

    Gen.choose(1, 50).flatMap { n =>
      Gen.listOfN(n, validChar).map(_.mkString)
    }
  }

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validNameString
    )

    behave like fieldWithPattern(
      form,
      fieldName,
      regexPattern.r,
      error = FormError(fieldName, invalidKey, Seq(regexPattern))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
