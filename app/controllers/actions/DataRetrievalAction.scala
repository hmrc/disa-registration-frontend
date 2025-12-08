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

import controllers.JourneyRecoveryController
import models.ErrorResponse

import javax.inject.Inject
import models.requests.{IdentifierRequest, OptionalDataRequest}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, ActionTransformer, Result, Results}
import services.JourneyAnswersService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class DataRetrievalActionImpl @Inject() (
  journeyAnswersService: JourneyAnswersService
)(implicit val executionContext: ExecutionContext)
    extends DataRetrievalAction {

  protected def refine[A](
    request: IdentifierRequest[A]
  ): Future[Either[Result, OptionalDataRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    journeyAnswersService.get(request.groupId).map {
      case Left(error: ErrorResponse) => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad().url))
      case Right(journeyData)         => Right(OptionalDataRequest(request.request, request.groupId, journeyData))
    }
  }
}

trait DataRetrievalAction extends ActionRefiner[IdentifierRequest, OptionalDataRequest]
