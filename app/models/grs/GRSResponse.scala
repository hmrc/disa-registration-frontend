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

import play.api.libs.json.{Format, JsResult, JsString, JsValue, Json, OFormat}

import java.time.LocalDate

case class GRSResponse(companyNumber: String,
                       companyName: Option[String],
                       ctutr: Option[String] = None,
                       chrn: Option[String] = None,
                       dateOfIncorporation: Option[LocalDate],
                       countryOfIncorporation: String = "GB",
                       identifiersMatch: Boolean,
                       businessRegistrationStatus: BusinessRegistrationStatus,
                       businessVerificationStatus: Option[BusinessVerificationStatus],
                       bpSafeId: Option[String])

object GRSResponse {
  implicit val format: OFormat[GRSResponse] = Json.format[GRSResponse]
}
