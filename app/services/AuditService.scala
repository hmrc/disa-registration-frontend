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

package services

import com.google.inject.Inject
import config.FrontendAppConfig
import models.SubmissionResult
import models.journeydata.JourneyData
import play.api.Logging
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import services.AuditTypes.{Audit, EnrolmentSubmitted}
import uk.gov.hmrc.auth.core.CredentialRole
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Disabled, Failure, Success}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.{ExecutionContext, Future}

class AuditService @Inject() (connector: AuditConnector, appConfig: FrontendAppConfig)(implicit ec: ExecutionContext)
    extends Logging {

  def auditEnrolmentSubmission(
    status: SubmissionResult,
    credentials: Credentials,
    credentialRole: CredentialRole,
    journeyData: JourneyData,
    failureReason: Option[String]
  )(implicit hc: HeaderCarrier): Future[Unit] = {
    val baseData = Json.obj(
      EventData.credId.toString           -> credentials.providerId,
      EventData.providerType.toString     -> credentials.providerType,
      EventData.internalRegId.toString    -> journeyData.enrolmentId,
      EventData.credentialRole.toString   -> credentialRole.toString,
      EventData.groupId.toString          -> journeyData.groupId,
      EventData.submissionStatus.toString -> status.toString,
      EventData.journeyData.toString      -> journeyData
    )

    val auditData: JsObject =
      failureReason match {
        case Some(r) if status == SubmissionResult.Failure =>
          baseData + (EventData.failureReason.toString -> JsString(r))
        case _                                             => baseData
      }

    val event = createAuditEvent(EnrolmentSubmitted, auditData)
    connector.sendExtendedEvent(event).map(logResponse(_, EnrolmentSubmitted.toString))
  }

  private def createAuditEvent(audit: Audit, auditData: JsValue): ExtendedDataEvent =
    ExtendedDataEvent(
      auditSource = appConfig.appName,
      auditType = audit.toString,
      detail = auditData
    )

  private def logResponse(result: AuditResult, auditType: String): Unit = result match {
    case Success         => logger.info(s"$auditType audit successful")
    case Failure(err, _) => logger.warn(s"$auditType Audit Error, message: $err")
    case Disabled        => logger.warn(s"$auditType failure - auditing disabled")
  }
}

object AuditTypes extends Enumeration {
  type Audit = Value
  val EnrolmentSubmitted = Value
}

object EventData extends Enumeration {
  type Data = Value
  val providerType, internalRegId, credId, credentialRole, groupId, submissionStatus, failureReason, journeyData = Value
}
