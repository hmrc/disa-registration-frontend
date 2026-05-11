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

class AddAnotherAddressFormProviderSpec extends StringFieldBehaviours {

  val form = new AddAnotherAddressFormProvider()()

  private val requiredKey = "AddAnotherAddress.postcode.error.required"

  ".postcode" - {

    "must bind valid postcodes" in {

      val result = form.bind(
        Map(
          "postcode" -> "SW1A 1AA",
          "filter"   -> "Test"
        )
      )

      result.value.value.postcode shouldBe "SW1A 1AA"
      result.value.value.filter   shouldBe Some("Test")
    }

    "must fail when postcode is missing" in {

      val result = form.bind(Map("filter" -> "Test"))

      result.errors should contain(
        FormError("postcode", requiredKey)
      )
    }

    "must fail when postcode is too short" in {

      val result = form.bind(
        Map("postcode" -> "AA1")
      )

      result.errors.exists(_.message == "AddAnotherAddress.postcode.error.tooShort") shouldBe true
    }

    "must fail when postcode is too long" in {

      val result = form.bind(
        Map("postcode" -> "AAAAAAAAAAA")
      )

      result.errors.exists(_.message == "AddAnotherAddress.postcode.error.tooLong") shouldBe true
    }

    "must fail when postcode is invalid format" in {

      val result = form.bind(
        Map("postcode" -> "INVALID123")
      )

      result.errors.exists(_.message == "AddAnotherAddress.postcode.error.invalid") shouldBe true
    }
  }

  ".filter" - {

    "must fail when filter exceeds max length" in {

      val longString = "a" * 256

      val result = form.bind(
        Map(
          "postcode" -> "SW1A 1AA",
          "filter"   -> longString
        )
      )

      result.errors.exists(_.message == "AddAnotherAddress.filter.error.incorrectLength") shouldBe true
    }

    "must bind valid filter when within limit" in {

      val result = form.bind(
        Map(
          "postcode" -> "SW1A 1AA",
          "filter"   -> "Flat 1"
        )
      )

      result.value.value.filter shouldBe Some("Flat 1")
    }

    "must allow empty filter" in {

      val result = form.bind(
        Map("postcode" -> "SW1A 1AA")
      )

      result.value.value.filter shouldBe None
    }
  }
}
