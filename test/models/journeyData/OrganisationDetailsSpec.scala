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

class OrganisationDetailsFormatSpec extends JsonFormatSpec[OrganisationDetails] {

  override val model =
    OrganisationDetails(
      registeredToManageIsa = Some(true),
      zRefNumber = Some("Z999"),
      fcaNumber = Some("F123"),
      correspondenceAddress = Some(CorrespondenceAddress(true, Some("123 Road"))),
      orgTelephoneNumber = Some("01111 222333")
    )

  override val json: JsValue = Json.parse("""
    {
      "registeredToManageIsa": true,
      "zRefNumber": "Z999",
      "fcaNumber": "F123",
      "correspondenceAddress": {
        "useThisAddress": true,
        "address": "123 Road"
      },
      "orgTelephoneNumber": "01111 222333"
    }
  """)

  override implicit val format: OFormat[OrganisationDetails] = OrganisationDetails.format
}
