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
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import play.api.data.FormError

class ChooseAddressFormProviderSpec extends StringFieldBehaviours {

  val form = new ChooseAddressFormProvider()()

  private val requiredKey = "chooseAddress.error.required"

  ".value" - {

    "must bind valid index values" in {

      val result = form.bind(
        Map(
          "value" -> "0"
        )
      )

      result.value.value shouldBe "0"
    }

    "must bind 'none' value correctly" in {

      val result = form.bind(
        Map(
          "value" -> "none"
        )
      )

      result.value.value shouldBe "none"
    }

    "must fail when value is missing" in {

      val result = form.bind(Map.empty[String, String])

      result.errors should contain(
        FormError("value", requiredKey)
      )
    }

    "must fail when value is empty string" in {

      val result = form.bind(
        Map("value" -> "")
      )

      result.errors should contain(
        FormError("value", requiredKey)
      )
    }

    "must bind higher index values" in {

      val result = form.bind(
        Map("value" -> "10")
      )

      result.value.value shouldBe "10"
    }
  }
}
