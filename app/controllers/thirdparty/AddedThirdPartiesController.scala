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

import config.FrontendAppConfig
import controllers.actions.*
import controllers.routes.*
import controllers.thirdparty.routes.*
import forms.YesNoAnswerFormProvider
import models.{NormalMode, YesNoAnswer}
import models.requests.DataRequest
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.thirdparty.AddedThirdPartiesSummary
import views.html.thirdparty.AddedThirdPartiesView

import javax.inject.Inject

class AddedThirdPartiesController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: YesNoAnswerFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: AddedThirdPartiesView,
  appConfig: FrontendAppConfig
)() extends FrontendBaseController
    with I18nSupport {

  val form: Form[YesNoAnswer] =
    formProvider("addedThirdParties.error.required")

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    getInProgressAndCompleteThirdParty.fold {
      Redirect(TaskListController.onPageLoad())
    } { case (inProgress, complete) =>
      Ok(view(form, AddedThirdPartiesSummary(inProgress, complete, appConfig.maxThirdParties)))
    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    getInProgressAndCompleteThirdParty.fold {
      Redirect(TaskListController.onPageLoad())
    } { case (inProgress, complete) =>
      val count = inProgress.size + complete.size

      if (count >= appConfig.maxThirdParties) {
        Redirect(TaskListController.onPageLoad())
      } else {
        form
          .bindFromRequest()
          .fold(
            formWithErrors =>
              BadRequest(
                view(formWithErrors, AddedThirdPartiesSummary(inProgress, complete, appConfig.maxThirdParties))
              ),
            {
              case YesNoAnswer.No =>
                if (count > 1) {
                  Redirect(TaskListController.onPageLoad())
                } else {
                  Redirect(TaskListController.onPageLoad())
                }
              case _              =>
                Redirect(ThirdPartyOrgDetailsController.onPageLoad(None, NormalMode))
            }
          )
      }
    }
  }

  private def getInProgressAndCompleteThirdParty(implicit request: DataRequest[_]) =
    request.journeyData.thirdPartyOrganisations
      .map(_.thirdParties)
      .filter(_.nonEmpty)
      .map(_.partition(_.inProgress))
}
