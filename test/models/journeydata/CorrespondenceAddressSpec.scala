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

package models.journeydata

import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json

class CorrespondenceAddressSpec
  extends AnyFreeSpec
    with Matchers
    with ScalaCheckPropertyChecks
    with OptionValues {

  "CorrespondenceAddress" - {

    "must serialise and deserialise with all fields present" in {

      val gen = for {
        line1 <- Gen.alphaStr.suchThat(_.nonEmpty)
        line2 <- Gen.alphaStr.suchThat(_.nonEmpty)
        line3 <- Gen.alphaStr.suchThat(_.nonEmpty)
        postCode <- Gen.alphaStr.suchThat(_.nonEmpty)
      } yield CorrespondenceAddress(
        Some(line1),
        Some(line2),
        Some(line3),
        Some(postCode)
      )

      forAll(gen) { address =>
        val json = Json.toJson(address)
        json.validate[CorrespondenceAddress].asOpt.value mustEqual address
      }
    }

    "must serialise and deserialise with empty optional fields" in {

      val address = CorrespondenceAddress()

      val json = Json.toJson(address)

      json.validate[CorrespondenceAddress].asOpt.value mustEqual address
    }

    "must handle partial fields" in {

      val address = CorrespondenceAddress(
        addressLine1 = Some("Line 1"),
        postCode = Some("AB1 2CD")
      )

      val json = Json.toJson(address)

      json.validate[CorrespondenceAddress].asOpt.value mustEqual address
    }
  }
}