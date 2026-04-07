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

package models

import models.grs.BusinessVerificationLockout
import play.api.libs.json.{Format, JsValue, Json}
import utils.JsonFormatSpec

import java.time.Instant

class BusinessVerificationLockoutSpec extends JsonFormatSpec[BusinessVerificationLockout] {

  private val testCreatedAt: Instant = Instant.parse("2026-01-01T00:00:00Z")

  override val model: BusinessVerificationLockout =
    BusinessVerificationLockout(
      ctutr = testString,
      groupId = Seq(testGroupId),
      createdAt = testCreatedAt
    )

  override val expectedJsonFromWrites: JsValue =
    Json.obj(
      "ctutr"     -> testString,
      "groupId"   -> Seq(testGroupId),
      "createdAt" -> Json.toJson(testCreatedAt)(BusinessVerificationLockout.instantFormat)
    )

  override implicit val format: Format[BusinessVerificationLockout] =
    BusinessVerificationLockout.format
}
