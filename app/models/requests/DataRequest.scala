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

package models.requests

import play.api.mvc.{Request, WrappedRequest}
import models.journeydata.JourneyData
import uk.gov.hmrc.auth.core.CredentialRole
import uk.gov.hmrc.auth.core.retrieve.Credentials

case class OptionalDataRequest[A](
  request: Request[A],
  groupId: String,
  credentials: Credentials,
  credentialRole: CredentialRole,
  journeyData: Option[JourneyData]
) extends WrappedRequest[A](request)

case class DataRequest[A](
  request: Request[A],
  groupId: String,
  credentials: Credentials,
  credentialRole: CredentialRole,
  journeyData: JourneyData
) extends WrappedRequest[A](request)

object DataRequest {
  def fromRequest[A](request: IdentifierRequest[A], journeyData: JourneyData): DataRequest[A] = DataRequest(
    request = request.request,
    groupId = request.groupId,
    credentials = request.credentials,
    credentialRole = request.credentialRole,
    journeyData = journeyData
  )
}
