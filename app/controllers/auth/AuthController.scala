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

package controllers.auth

import config.FrontendAppConfig
import controllers.actions.IdentifierAction
import models.session.Session
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  config: FrontendAppConfig,
  identify: IdentifierAction,
  sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def signOut(): Action[AnyContent] = identify.async { implicit request =>
    sessionRepository
      .findAndDelete(request.credentials.providerId)
      .recover { case e =>
        logger.warn(s"Failed to find and clear session for userId: [${request.credentials.providerId}]", e)
        None
      }
      .flatMap {
        case Some(Session(_, _, true, _)) =>
          Future.successful(
            Redirect(
              config.signOutUrl,
              Map("continue" -> Seq(s"${routes.SignedOutController.signOut().url}"))
            )
          )
        case _                            =>
          Future.successful(
            Redirect(
              config.signOutUrl,
              Map("continue" -> Seq(s"${routes.SignedOutController.signOutAnswersNotSaved().url}"))
            )
          )
      }
  }
}
