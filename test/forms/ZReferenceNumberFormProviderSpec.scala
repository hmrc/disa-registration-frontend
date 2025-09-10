/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.data.Form

final class ZReferenceNumberFormProviderSpec extends AnyWordSpec with Matchers {

  private val form: Form[String] = new ZReferenceNumberFormProvider()()

  "ZReferenceNumberFormProvider" should {

    "bind a valid Z-ref like Z1234" in {
      val result = form.bind(Map("value" -> "Z1234"))
      result.errors mustBe Nil
      result.value.get mustBe "Z1234"
    }

    "bind a valid Z-ref with whitespace" in {
      val result = form.bind(Map("value" -> "   Z1234   "))
      result.errors mustBe Nil
      result.value.get mustBe "Z1234"
    }

    "fail when missing" in {
      val result = form.bind(Map("value" -> ""))
      result.errors.map(_.message) must contain("orgDetails.zReferenceNumber.error.missing")
    }

    "fail for lowercase z" in {
      val result = form.bind(Map("value" -> "z1234"))
      result.errors.map(_.message) must contain("orgDetails.zReferenceNumber.error.invalid")
    }

    "fail for wrong length" in {
      form.bind(Map("value" -> "Z12")).errors.map(_.message)    must contain("orgDetails.zReferenceNumber.error.invalid")
      form.bind(Map("value" -> "Z12345")).errors.map(_.message) must contain(
        "orgDetails.zReferenceNumber.error.invalid"
      )
    }

    "fail for wrong prefix" in {
      val result = form.bind(Map("value" -> "A1234"))
      result.errors.map(_.message) must contain("orgDetails.zReferenceNumber.error.invalid")
    }
  }
}
