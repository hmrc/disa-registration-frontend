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

import models.grs.CreateJourneyResponse
import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json

class CreateJourneyResponseSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "CreateJourneyResponse" - {

    "must deserialise valid values" in {

      val gen = for {
        url <- Gen.alphaStr.suchThat(_.nonEmpty)
      } yield CreateJourneyResponse(url)

      forAll(gen) { response =>
        val json = Json.obj(
          "journeyStartUrl" -> response.journeyStartUrl
        )

        json.validate[CreateJourneyResponse].asOpt.value mustEqual response
      }
    }

    "must serialise" in {

      val gen = for {
        url <- Gen.alphaStr.suchThat(_.nonEmpty)
      } yield CreateJourneyResponse(url)

      forAll(gen) { response =>
        Json.toJson(response) mustEqual Json.obj(
          "journeyStartUrl" -> response.journeyStartUrl
        )
      }
    }
  }
}
