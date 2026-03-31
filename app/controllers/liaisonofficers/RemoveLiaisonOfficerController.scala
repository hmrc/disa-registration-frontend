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

package controllers.liaisonofficers

import controllers.actions.*
import controllers.routes.IndexController
import forms.YesNoAnswerFormProvider
import handlers.ErrorHandler
import models.NormalMode
import models.YesNoAnswer.{No, Yes}
import models.journeydata.liaisonofficers.{LiaisonOfficer, LiaisonOfficers}
import models.requests.DataRequest
import navigation.Navigator
import pages.liaisonofficers.RemoveLiaisonOfficerPage
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.liaisonofficers.RemoveLiaisonOfficerView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class RemoveLiaisonOfficerController @Inject() (
  override val messagesApi: MessagesApi,
  journeyAnswersService: JourneyAnswersService,
  navigator: Navigator,
  errorHandler: ErrorHandler,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: YesNoAnswerFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: RemoveLiaisonOfficerView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider("removeLiaisonOfficer.error.required")

  def onPageLoad(id: String): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      providingName(id, name => Future.successful(Ok(view(id, name, form))))
  }

  def onSubmit(id: String): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      providingName(
        id,
        name =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(id, name, formWithErrors))),
              value => {
                val updatedSection = value match {
                  case Yes => updatedSectionWithLoRemoved(id)
                  case No  => request.journeyData.liaisonOfficers
                }

                updatedSection.fold(Future.successful(Redirect(IndexController.onPageLoad())))(updateAnswersAndRedirect)
              }
            )
      )
  }

  private def providingName(id: String, block: String => Future[Result])(implicit request: DataRequest[_]) =
    (for {
      lo   <- findLiaisonOfficer(id)
      name <- lo.fullName
    } yield block(name))
      .getOrElse(Future.successful(Redirect(IndexController.onPageLoad())))

  private def updateAnswersAndRedirect(
    updatedSection: LiaisonOfficers
  )(implicit request: DataRequest[_], executionContext: ExecutionContext) =
    journeyAnswersService
      .update(updatedSection, request.groupId, request.credentials.providerId)
      .map { updatedSection =>
        Redirect(navigator.nextPage(RemoveLiaisonOfficerPage, updatedSection, NormalMode))
      }
      .recoverWith { case NonFatal(e) =>
        logger.warn(
          s"Failed updating answers for section [${LiaisonOfficers.sectionName}] for groupId [${request.groupId}] with error: [$e]"
        )
        errorHandler.internalServerError
      }

  private def findLiaisonOfficer(id: String)(implicit request: DataRequest[_]): Option[LiaisonOfficer] =
    request.journeyData.liaisonOfficers.flatMap(_.liaisonOfficers.find(_.id == id))

  private def updatedSectionWithLoRemoved(id: String)(implicit request: DataRequest[_]): Option[LiaisonOfficers] =
    request.journeyData.liaisonOfficers.map(liaisonOfficers =>
      liaisonOfficers.copy(
        liaisonOfficers = liaisonOfficers.liaisonOfficers.filterNot(_.id == id)
      )
    )
}
