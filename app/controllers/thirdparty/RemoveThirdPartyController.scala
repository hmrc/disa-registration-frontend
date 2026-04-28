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

package controllers.thirdparty

import controllers.actions.*
import controllers.routes.IndexController
import forms.YesNoAnswerFormProvider
import handlers.ErrorHandler
import models.YesNoAnswer.{No, Yes}
import models.journeydata.thirdparty.{ThirdParty, ThirdPartyOrganisations}
import models.requests.DataRequest
import models.{NormalMode, YesNoAnswer}
import navigation.Navigator
import pages.thirdparty.RemoveThirdPartyPage
import play.api.data.Form
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.thirdparty.RemoveThirdPartyView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class RemoveThirdPartyController @Inject() (
  override val messagesApi: MessagesApi,
  journeyAnswersService: JourneyAnswersService,
  navigator: Navigator,
  errorHandler: ErrorHandler,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: YesNoAnswerFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: RemoveThirdPartyView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[YesNoAnswer] = formProvider("removeThirdParty.error.required")

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
                  case Yes => updatedSectionWithThirdPartyRemoved(id)
                  case No  => request.journeyData.thirdPartyOrganisations
                }

                updatedSection.fold(Future.successful(Redirect(IndexController.onPageLoad())))(section =>
                  updateAnswersAndRedirect(section)
                )
              }
            )
      )
  }

  private def providingName(id: String, block: String => Future[Result])(implicit request: DataRequest[_]) =
    (for {
      thirdParty <- findThirdParty(id)
      name       <- thirdParty.thirdPartyName
    } yield block(name))
      .getOrElse(Future.successful(Redirect(IndexController.onPageLoad())))

  private def updateAnswersAndRedirect(
    updatedSection: ThirdPartyOrganisations
  )(implicit request: DataRequest[_], executionContext: ExecutionContext) =
    journeyAnswersService
      .update(updatedSection, request.groupId, request.credentials.providerId)
      .map { updatedSection =>
        Redirect(navigator.nextPage(RemoveThirdPartyPage, updatedSection, NormalMode))
      }
      .recoverWith { case NonFatal(e) =>
        logger.warn(
          s"Failed updating answers for section [${ThirdPartyOrganisations.sectionName}] for groupId [${request.groupId}] with error: [$e]"
        )
        errorHandler.internalServerError
      }

  private def findThirdParty(id: String)(implicit request: DataRequest[_]): Option[ThirdParty] =
    request.journeyData.thirdPartyOrganisations.flatMap(_.thirdParties.find(_.id == id))

  private def updatedSectionWithThirdPartyRemoved(
    id: String
  )(implicit request: DataRequest[_]): Option[ThirdPartyOrganisations] =
    request.journeyData.thirdPartyOrganisations.map(thirdParties =>
      thirdParties.copy(
        thirdParties = thirdParties.thirdParties.filterNot(_.id == id)
      )
    )
}
