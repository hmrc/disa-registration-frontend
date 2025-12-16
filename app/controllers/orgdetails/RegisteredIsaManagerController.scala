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

package controllers.orgdetails

import controllers.actions.*
import forms.RegisteredIsaManagerFormProvider
import handlers.ErrorHandler
import models.Mode
import models.journeydata.OrganisationDetails
import navigation.Navigator
import pages.*
import play.api.data.Form
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.orgdetails.RegisteredIsaManagerView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegisteredIsaManagerController @Inject() (
  override val messagesApi: MessagesApi,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  formProvider: RegisteredIsaManagerFormProvider,
  journeyAnswersService: JourneyAnswersService,
  errorHandler: ErrorHandler,
  val controllerComponents: MessagesControllerComponents,
  view: RegisteredIsaManagerView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData) { implicit request =>
    val preparedForm = request.journeyData.fold(form)(_.organisationDetails.fold(form)(_.registeredToManageIsa match {
      case None        => form
      case Some(value) => form.fill(value)
    }))

    Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
        answer => {
          val updatedSection =
            request.journeyData.flatMap(_.organisationDetails) match {
              case Some(existing) => existing.copy(registeredToManageIsa = Some(answer))
              case None           => OrganisationDetails(registeredToManageIsa = Some(answer))
            }

          journeyAnswersService
            .update(updatedSection, request.groupId)
            .map { _ =>
              Redirect(navigator.nextPage(RegisteredIsaManagerPage, mode))
            }
            .recoverWith { case e =>
              logger.warn(
                s"Failed updating answers for section [${updatedSection.sectionName}] for groupId [${request.groupId}] with error: [$e]"
              )
              errorHandler.internalServerError
            }
        }
      )
  }
}
