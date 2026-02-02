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

import connectors.DisaRegistrationConnector
import models.SubmissionResult.{Failure, Success}
import models.journeydata.JourneyData
import uk.gov.hmrc.auth.core.CredentialRole
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmissionService @Inject() (connector: DisaRegistrationConnector, auditService: AuditService) {

  def declareAndSubmit(credentials: Credentials, credentialRole: CredentialRole, journeyData: JourneyData)(implicit
    executionContext: ExecutionContext,
    hc: HeaderCarrier
  ): Future[String] =
    connector
      .declareAndSubmit(journeyData.groupId)
      .map(receiptId =>
        auditService.auditEnrolmentSubmission(Success, credentials, credentialRole, journeyData, None)
        receiptId
      )
      .recoverWith { case e: Throwable =>
        auditService.auditEnrolmentSubmission(Failure, credentials, credentialRole, journeyData, Some(e.getMessage))
        Future.failed(e)
      }
}
