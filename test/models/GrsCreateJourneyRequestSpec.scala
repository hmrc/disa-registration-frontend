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

package models.grs

import play.api.libs.json.{Format, JsValue, Json}
import utils.JsonFormatSpec

class GrsCreateJourneyRequestSpec extends JsonFormatSpec[GrsCreateJourneyRequest] {

  val testJourneyRequest: GrsCreateJourneyRequest = GrsCreateJourneyRequest(
    continueUrl = "http://localhost/continue",
    businessVerificationCheck = true,
    deskProServiceId = "deskProId",
    signOutUrl = "/some/url",
    regime = "ISA",
    accessibilityUrl = "/some/url",
    labels = Some(Labels(en = Some(ServiceLabel("serviceLabel"))))
  )

  override val model: GrsCreateJourneyRequest = testJourneyRequest

  override val json: JsValue = Json.parse("""
    {
      "continueUrl": "http://localhost/continue",
      "businessVerificationCheck": true,
      "deskProServiceId": "deskProId",
      "signOutUrl": "/some/url",
      "regime": "ISA",
      "accessibilityUrl": "/some/url",
      "labels": {
        "en": {
          "optServiceName": "serviceLabel"
        }
      }
    }
  """)

  override implicit val format: Format[GrsCreateJourneyRequest] = GrsCreateJourneyRequest.format
}
