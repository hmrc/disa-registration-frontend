package controllers

import controllers.actions.{DataRetrievalAction, IdentifierAction}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class KeepAliveController @Inject() (
                                      val controllerComponents: MessagesControllerComponents,
                                      identify: IdentifierAction,
                                      sessionRepository: SessionRepository
                                    )(implicit ec: ExecutionContext)
  extends FrontendBaseController {

  def keepAlive(): Action[AnyContent] = identify.async { implicit request =>
    sessionRepository.keepAlive(request.groupId).map(_ => Ok)
  }
}