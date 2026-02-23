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

package models.session

import play.api.libs.json.{Format, JsValue, Json}
import utils.JsonFormatSpec

import java.time.Instant

class SessionSpec extends JsonFormatSpec[Session] {

  def model: Session = Session(testGroupId, true, Instant.parse("2025-10-21T10:00:00Z"))

  def expectedJsonFromWrites: JsValue =
    Json.parse("""{
         | "userId": "3147318d-1cd9-4534-a4e8-ae268ea923ed",
         | "auditContinuationEventSent": true,
         | "lastSeen":{"$date":{"$numberLong":"1761040800000"}}
         |}""".stripMargin)

  implicit def format: Format[Session] = Session.format
}
