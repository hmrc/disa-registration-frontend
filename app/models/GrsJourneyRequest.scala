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

package models

import play.api.libs.json.{Json, OFormat}

case class LabelsLanguage(optServiceName: String)

case class Labels(
                   cy: LabelsLanguage,
                   en: LabelsLanguage
                 )

case class GrsJourneyRequest(
                              continueUrl: String,
                              businessVerificationCheck: Boolean,
                              optServiceName: Option[String],
                              deskProServiceId: String,
                              signOutUrl: String,
                              regime: String,
                              accessibilityUrl: String,
                              labels: Option[Labels]
                            )

object GrsJourneyRequest {
  implicit val labelsLanguageFormat: OFormat[LabelsLanguage] = Json.format[LabelsLanguage]
  implicit val labelsFormat: OFormat[Labels] = Json.format[Labels]
  implicit val format: OFormat[GrsJourneyRequest] = Json.format[GrsJourneyRequest]
}
