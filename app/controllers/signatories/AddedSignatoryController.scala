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

import config.FrontendAppConfig
import controllers.actions.*
import controllers.routes.*
import controllers.signatories.routes.*
import forms.YesNoAnswerFormProvider
import models.requests.DataRequest
import models.{NormalMode, YesNoAnswer}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.AddedSignatoriesSummary
import views.html.signatories.AddedSignatoryView

import javax.inject.Inject

class AddedSignatoryController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          identify: IdentifierAction,
                                          getData: DataRetrievalAction,
                                          requireData: DataRequiredAction,
                                          formProvider: YesNoAnswerFormProvider,
                                          val controllerComponents: MessagesControllerComponents,
                                          view: AddedSignatoryView,
                                          appConfig: FrontendAppConfig
                                        )()
  extends FrontendBaseController
    with I18nSupport {

  val form: Form[YesNoAnswer] =
    formProvider("addedSignatory.error.required")


  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    getInProgressAndCompleteSignatories.fold {
      Redirect(TaskListController.onPageLoad())
    } { case (inProgress, complete) =>
      Ok(view(form, AddedSignatoriesSummary(inProgress, complete, appConfig.maxSignatories)))
    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    getInProgressAndCompleteSignatories.fold {
      Redirect(TaskListController.onPageLoad())
    } { case (inProgress, complete) =>
      val count = inProgress.size + complete.size
      form
        .bindFromRequest()
        .fold(
          formWithErrors => BadRequest(view(formWithErrors, AddedSignatoriesSummary(inProgress, complete, appConfig.maxSignatories))),
          {
            case YesNoAnswer.Yes if count < appConfig.maxSignatories =>
              Redirect(SignatoryNameController.onPageLoad(None, NormalMode)
              )
            case _ => Redirect(TaskListController.onPageLoad())
          }
        )
    }
  }

  private def getInProgressAndCompleteSignatories(implicit request: DataRequest[_]) =
    request.journeyData.signatories
      .map(_.signatories)
      .filter(_.nonEmpty)
      .map(_.partition(_.inProgress))
}