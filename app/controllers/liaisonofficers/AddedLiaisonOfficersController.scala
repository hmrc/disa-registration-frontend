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

import config.FrontendAppConfig
import controllers.actions.*
import controllers.liaisonofficers.routes.LiaisonOfficerNameController
import controllers.routes.TaskListController
import forms.YesNoAnswerFormProvider
import models.requests.DataRequest
import models.{Mode, NormalMode, YesNoAnswer}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.liaisonofficers.AddedLiaisonOfficerSummary
import views.html.liaisonofficers.AddedLiaisonOfficersView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AddedLiaisonOfficersController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: YesNoAnswerFormProvider,
  appConfig: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents,
  view: AddedLiaisonOfficersView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[YesNoAnswer] = formProvider("addedLiaisonOfficers.error.required")

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    getInProgressAndCompleteLos.fold {
      Redirect(TaskListController.onPageLoad())
    } { case (inProgress, complete) =>
      Ok(view(form, AddedLiaisonOfficerSummary(inProgress, complete), mode))
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    getInProgressAndCompleteLos.fold {
      Redirect(TaskListController.onPageLoad())
    } { case (inProgress, complete) =>
      val count = inProgress.size + complete.size
      form
        .bindFromRequest()
        .fold(
          formWithErrors => BadRequest(view(formWithErrors, AddedLiaisonOfficerSummary(inProgress, complete), mode)),
          {
            case YesNoAnswer.Yes if count < appConfig.maxLos =>
              Redirect(LiaisonOfficerNameController.onPageLoad(None, NormalMode))
            case _                                           => Redirect(TaskListController.onPageLoad())
          }
        )
    }
  }

  private def getInProgressAndCompleteLos(implicit request: DataRequest[_]) =
    request.journeyData.liaisonOfficers
      .map(_.liaisonOfficers)
      .filter(_.nonEmpty)
      .map(_.partition(_.inProgress))
}
