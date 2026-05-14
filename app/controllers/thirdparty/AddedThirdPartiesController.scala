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

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import controllers.routes.TaskListController
import forms.YesNoAnswerFormProvider
import models.requests.DataRequest
import models.{Mode, ReturnTo, YesNoAnswer}
import navigation.Navigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.thirdparty.AddedThirdPartiesSummary
import views.html.thirdparty.AddedThirdPartiesView

class AddedThirdPartiesController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: YesNoAnswerFormProvider,
  navigator: Navigator,
  val controllerComponents: MessagesControllerComponents,
  view: AddedThirdPartiesView,
  appConfig: FrontendAppConfig
) extends FrontendBaseController
    with I18nSupport {

  val form: Form[YesNoAnswer] =
    formProvider("addedThirdParties.error.required")

  def onPageLoad(mode: Mode, returnTo: Option[ReturnTo]): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      getInProgressAndCompleteThirdParty.fold {
        Redirect(TaskListController.onPageLoad())
      } { case (inProgress, complete) =>
        Ok(
          view(
            form,
            AddedThirdPartiesSummary(inProgress, complete, appConfig.maxThirdParties),
            mode,
            returnTo
          )
        )
      }
    }

  def onSubmit(mode: Mode, returnTo: Option[ReturnTo]): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      getInProgressAndCompleteThirdParty.fold {
        Redirect(TaskListController.onPageLoad())
      } { case (inProgress, complete) =>
        val count                  = inProgress.size + complete.size
        val connectedOrganisations =
          request.journeyData.thirdPartyOrganisations.map(_.connectedOrganisations).fold(Nil)(identity)

        if (count == appConfig.maxThirdParties) {
          Redirect(
            navigator.nextPageFromAddedThirdParties(YesNoAnswer.No, count, connectedOrganisations, mode, returnTo)
          )
        } else {
          form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                BadRequest(
                  view(
                    formWithErrors,
                    AddedThirdPartiesSummary(inProgress, complete, appConfig.maxThirdParties),
                    mode,
                    returnTo
                  )
                ),
              answer =>
                Redirect(navigator.nextPageFromAddedThirdParties(answer, count, connectedOrganisations, mode, returnTo))
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
