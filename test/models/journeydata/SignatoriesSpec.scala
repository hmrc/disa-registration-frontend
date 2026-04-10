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
import models.journeydata.signatories.{Signatories, Signatory}
import play.api.libs.json.{JsValue, Json}

class SignatoriesSpec extends SpecBase {

  private val signatory1 = Signatory("id-1", Some("Jane Smith"), Some("Director"))
  private val signatory2 = Signatory("id-2", Some("John Doe"), Some("Manager"))

  "Signatories" - {

    "must serialise to JSON correctly" in {

      val model = Signatories(Seq(signatory1, signatory2))

      val expectedJson: JsValue = Json.parse(
        """
          |{
          |  "signatories": [
          |    {
          |      "id": "id-1",
          |      "fullName": "Jane Smith",
          |      "jobTitle": "Director"
          |    },
          |    {
          |      "id": "id-2",
          |      "fullName": "John Doe",
          |      "jobTitle": "Manager"
          |    }
          |  ]
          |}
          |""".stripMargin
      )

      Json.toJson(model) mustEqual expectedJson
    }

    "must deserialise from JSON correctly" in {

      val json: JsValue = Json.parse(
        """
          |{
          |  "signatories": [
          |    {
          |      "id": "id-1",
          |      "fullName": "Jane Smith",
          |      "jobTitle": "Director"
          |    },
          |    {
          |      "id": "id-2",
          |      "fullName": "John Doe",
          |      "jobTitle": "Manager"
          |    }
          |  ]
          |}
          |""".stripMargin
      )

      json.as[Signatories] mustEqual Signatories(Seq(signatory1, signatory2))
    }

    "must default to empty sequence when not provided" in {

      val model = Signatories()

      model.signatories mustBe empty
    }

    "must have the correct section name" in {

      val model = Signatories()

      model.sectionName mustEqual "signatories"
    }

    "must handle empty JSON array" in {

      val json: JsValue = Json.parse(
        """
          |{
          |  "signatories": []
          |}
          |""".stripMargin
      )

      json.as[Signatories] mustEqual Signatories(Seq.empty)
    }
  }
}
