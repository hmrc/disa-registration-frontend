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

package controllers.signatories

import controllers.actions.*
import controllers.routes.IndexController
import forms.YesNoAnswerFormProvider
import handlers.ErrorHandler
import models.NormalMode
import models.YesNoAnswer.{No, Yes}
import models.journeydata.signatories.{Signatories, Signatory}
import models.requests.DataRequest
import navigation.Navigator
import pages.signatories.RemoveSignatoryPage
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.signatories.RemoveSignatoryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class RemoveSignatoryController @Inject()(
  override val messagesApi: MessagesApi,
  journeyAnswersService: JourneyAnswersService,
  navigator: Navigator,
  errorHandler: ErrorHandler,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: YesNoAnswerFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: RemoveSignatoryView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider("removeSignatory.error.required")

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
                  case Yes => updatedSectionWithSignatoryRemoved(id)
                  case No  => request.journeyData.signatories
                }

                updatedSection.fold(Future.successful(Redirect(IndexController.onPageLoad())))(updateAnswersAndRedirect)
              }
            )
      )
  }

  private def providingName(id: String, block: String => Future[Result])(implicit request: DataRequest[_]) =
    (for {
      signatory   <- findSignatory(id)
      name <- signatory.fullName
    } yield block(name))
      .getOrElse(Future.successful(Redirect(IndexController.onPageLoad())))

  private def updateAnswersAndRedirect(
    updatedSection: Signatories
  )(implicit request: DataRequest[_], executionContext: ExecutionContext) =
    journeyAnswersService
      .update(updatedSection, request.groupId, request.credentials.providerId)
      .map { updatedSection =>
        Redirect(navigator.nextPage(RemoveSignatoryPage, updatedSection, NormalMode))
      }
      .recoverWith { case NonFatal(e) =>
        logger.warn(
          s"Failed updating answers for section [${Signatories.sectionName}] for groupId [${request.groupId}] with error: [$e]"
        )
        errorHandler.internalServerError
      }

  private def findSignatory(id: String)(implicit request: DataRequest[_]): Option[Signatory] =
    request.journeyData.signatories.flatMap(_.signatories.find(_.id == id))

  private def updatedSectionWithSignatoryRemoved(id: String)(implicit request: DataRequest[_]): Option[Signatories] =
    request.journeyData.signatories.map(signatories =>
      signatories.copy(
        signatories = signatories.signatories.filterNot(_.id == id)
      )
    )
}
