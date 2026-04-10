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

import base.SpecBase
import models.journeydata.signatories.Signatory
import play.api.libs.json.{JsValue, Json}

class SignatorySpec extends SpecBase {

  "Signatory" - {

    "must serialise to JSON correctly when all fields are present" in {

      val model = Signatory(
        id = "id-123",
        fullName = Some("Jane Smith"),
        jobTitle = Some("Director")
      )

      val expectedJson: JsValue = Json.parse(
        """
          |{
          |  "id": "id-123",
          |  "fullName": "Jane Smith",
          |  "jobTitle": "Director"
          |}
          |""".stripMargin
      )

      Json.toJson(model) mustEqual expectedJson
    }

    "must deserialise from JSON correctly when all fields are present" in {

      val json: JsValue = Json.parse(
        """
          |{
          |  "id": "id-123",
          |  "fullName": "Jane Smith",
          |  "jobTitle": "Director"
          |}
          |""".stripMargin
      )

      json.as[Signatory] mustEqual Signatory(
        id = "id-123",
        fullName = Some("Jane Smith"),
        jobTitle = Some("Director")
      )
    }

    "must handle missing optional fields when deserialising" in {

      val json: JsValue = Json.parse(
        """
          |{
          |  "id": "id-123"
          |}
          |""".stripMargin
      )

      json.as[Signatory] mustEqual Signatory(
        id = "id-123",
        fullName = None,
        jobTitle = None
      )
    }

    "must serialise correctly when optional fields are empty" in {

      val model = Signatory(
        id = "id-123",
        fullName = None,
        jobTitle = None
      )

      val expectedJson: JsValue = Json.parse(
        """
          |{
          |  "id": "id-123"
          |}
          |""".stripMargin
      )

      Json.toJson(model) mustEqual expectedJson
    }
  }
}
