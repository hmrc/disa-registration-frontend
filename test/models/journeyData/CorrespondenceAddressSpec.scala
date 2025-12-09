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

class CorrespondenceAddressFormatSpec extends JsonFormatSpec[CorrespondenceAddress] {

  override val model =
    CorrespondenceAddress(
      useThisAddress = true,
      address = Some("123 Main Street")
    )

  override val json: JsValue = Json.parse("""
    {
      "useThisAddress": true,
      "address": "123 Main Street"
    }
  """)

  override implicit val format: OFormat[CorrespondenceAddress] = CorrespondenceAddress.format
}
