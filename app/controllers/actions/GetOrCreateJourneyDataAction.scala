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

import handlers.ErrorHandler
import models.requests.{DataRequest, IdentifierRequest}
import play.api.Logging
import play.api.mvc.{ActionRefiner, Result}
import services.{AuditService, JourneyAnswersService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetOrCreateJourneyDataActionImpl @Inject() (
  journeyAnswersService: JourneyAnswersService,
  auditService: AuditService,
  errorHandler: ErrorHandler
)(implicit val executionContext: ExecutionContext)
    extends GetOrCreateJourneyDataAction
    with Logging {

  protected def refine[A](
    request: IdentifierRequest[A]
  ): Future[Either[Result, DataRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    journeyAnswersService
      .getOrCreateJourneyData(request.groupId)
      .flatMap { response =>
        if (response.isNewEnrolmentJourney)
          auditService
            .auditNewEnrolmentStarted(
              request.credentials,
              request.credentialRole,
              response.journeyData.enrolmentId,
              request.groupId
            )
            .recover { case e =>
              logger.warn(s"Failed to audit EnrolmentStarted for groupId [${request.groupId}]", e)
            }

        Future.successful(
          Right(
            DataRequest(
              request.request,
              request.groupId,
              request.credentials,
              request.credentialRole,
              response.journeyData
            )
          )
        )
      }
      .recoverWith { case e: Throwable =>
        logger.error(s"Failed to getOrCreateJourneyData for groupId: [${request.groupId}]", e)
        errorHandler.internalServerError(request).map(Left.apply)
      }
  }
}

trait GetOrCreateJourneyDataAction extends ActionRefiner[IdentifierRequest, DataRequest]
