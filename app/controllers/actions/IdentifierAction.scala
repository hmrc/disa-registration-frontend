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

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.mvc.*
import play.api.mvc.Results.*
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.*
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction
    extends ActionBuilder[IdentifierRequest, AnyContent]
    with ActionFunction[Request, IdentifierRequest]

class AuthenticatedIdentifierAction @Inject() (
  override val authConnector: AuthConnector,
  config: FrontendAppConfig,
  sessionRepository: SessionRepository,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends IdentifierAction
    with AuthorisedFunctions
    with Logging {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(groupIdentifier and affinityGroup and credentials and credentialRole) {
      case Some(groupId) ~ Some(Organisation) ~ Some(credentials) ~ Some(role) =>
        sessionRepository
          .keepAlive(credentials.providerId)
          .recover { case e => logger.warn(s"Failed to keep session alive for userId: [${credentials.providerId}]", e) }
          .flatMap(_ => block(IdentifierRequest(request, groupId, credentials, role)))
      case _ ~ _ ~ None ~ _ | _ ~ _ ~ _ ~ None                                 =>
        logger.warn(s"Authorisation failed due to missing credentials or credentialRole")
        Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
      case Some(_) ~ Some(affinity) ~ _ ~ _                                    =>
        Future.successful(
          Redirect(routes.UnsupportedAffinityGroupController.onPageLoad(affinityGroup = affinity.toString))
        )
      case _                                                                   =>
        Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
    }
  } recover {
    case _: NoActiveSession        =>
      Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
    case _: AuthorisationException =>
      Redirect(routes.UnauthorisedController.onPageLoad())
  }
}
