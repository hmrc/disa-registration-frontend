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

class OutsourcedAdministrationFormatSpec extends JsonFormatSpec[OutsourcedAdministration] {

  override val model =
    OutsourcedAdministration(
      dataItem = Some("foo"),
      dataItem2 = Some("bar")
    )

  override val json: JsValue = Json.parse("""
    {
      "dataItem": "foo",
      "dataItem2": "bar"
    }
  """)

  override implicit val format: OFormat[OutsourcedAdministration] = OutsourcedAdministration.format
}
