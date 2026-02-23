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

package controllers.actions

import models.requests.DataRequest
import play.api.Logging
import play.api.mvc.ActionTransformer
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuditContinuationActionImpl @Inject() (
  sessionRepository: SessionRepository,
  auditService: AuditService
)(implicit val ec: ExecutionContext)
    extends AuditContinuationAction
    with Logging {

  override def apply(sectionName: String): ActionTransformer[DataRequest, DataRequest] =
    new ActionTransformer[DataRequest, DataRequest] {
      protected def executionContext: ExecutionContext = ec

      protected def transform[A](request: DataRequest[A]): Future[DataRequest[A]] = {
        implicit val hc: HeaderCarrier =
          HeaderCarrierConverter.fromRequestAndSession(request, request.session)

        sessionRepository
          .getOrCreateSessionAndMarkAuditEventSent(request.credentials.providerId)
          .flatMap {
            case true =>
              auditService
                .auditContinuation(request, sectionName)
                .recover { case e =>
                  logger.warn(s"auditContinuation failed for groupId: [${request.groupId}]", e)
                }
                .map(_ => request)

            case false =>
              Future.successful(request)
          }
          .recover { case e =>
            logger.warn(s"AuditContinuationAction failed for groupId: [${request.groupId}]", e)
            request
          }
      }
    }
}

trait AuditContinuationAction {
  def apply(sectionName: String): ActionTransformer[DataRequest, DataRequest]
}
