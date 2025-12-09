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

package controllers.actions

import models.requests.{IdentifierRequest, OptionalDataRequest}
import play.api.Logging
import play.api.mvc.Results.InternalServerError
import play.api.mvc.{ActionRefiner, Result, Results}
import services.JourneyAnswersService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DataRetrievalActionImpl @Inject() (
  journeyAnswersService: JourneyAnswersService
)(implicit val executionContext: ExecutionContext)
    extends DataRetrievalAction
    with Logging {

  protected def refine[A](
    request: IdentifierRequest[A]
  ): Future[Either[Result, OptionalDataRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    journeyAnswersService
      .get(request.groupId)
      .map { journeyData =>
        Right(OptionalDataRequest(request.request, request.groupId, journeyData))
      }
      .recover { case e: Throwable =>
        logger.warn(s"Failed to retrieve answers for user with groupId: [${request.groupId}] with error: [$e]")
        Left(InternalServerError)
      }
  }
}

trait DataRetrievalAction extends ActionRefiner[IdentifierRequest, OptionalDataRequest]
