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
import org.scalacheck.Gen
import play.api.data.FormError

import scala.util.matching.Regex

class ThirdPartyInvestorFundsPercentageFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "thirdPartyInvestorFundsPercentage.error.required"
  val invalidKey  = "thirdPartyInvestorFundsPercentage.error.invalid"

  val pattern: Regex = """^[0-9]+$""".r

  val form = new ThirdPartyInvestorFundsPercentageFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      Gen.oneOf("0", "1", "50", "99", "100")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must not bind non-numeric values" in {

      val invalidValues = Seq("abc", "10.5", "-1", "1a", "ten")

      invalidValues.foreach { value =>
        val result = form.bind(Map(fieldName -> value))
        result.errors must contain(FormError(fieldName, invalidKey))
      }
    }

    "must not bind values below 0 or above 100" in {

      val invalidValues = Seq("-10", "101", "999")

      invalidValues.foreach { value =>
        val result = form.bind(Map(fieldName -> value))
        result.errors must contain(FormError(fieldName, invalidKey))
      }
    }

    "must bind valid whole numbers between 0 and 100 inclusive" in {

      val validValues = Seq("1", "50")

      validValues.foreach { value =>
        val result = form.bind(Map(fieldName -> value))
        result.errors mustBe empty
      }
    }
  }
}
