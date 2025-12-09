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

package models.journeyData

import play.api.libs.json.{JsValue, Json, OFormat}
import utils.JsonFormatSpec

class LiaisonOfficersFormatSpec extends JsonFormatSpec[LiaisonOfficers] {

  override val model =
    LiaisonOfficers(
      dataItem = Some("a"),
      dataItem2 = Some("b")
    )

  override val json: JsValue = Json.parse("""
    {
      "dataItem": "a",
      "dataItem2": "b"
    }
  """)

  override implicit val format: OFormat[LiaisonOfficers] = LiaisonOfficers.format
}
