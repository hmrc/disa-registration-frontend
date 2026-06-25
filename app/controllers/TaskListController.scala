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

package controllers

import controllers.actions.*
import models.journeydata.TaskListProgress
import navigation.TaskListRoutes
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AuditService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.tasklist.TaskListViewModel
import views.html.TaskListView

import javax.inject.Inject

class TaskListController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  getUprn: EnrichRegisteredAddressUprnAction,
  requireData: DataRequiredAction,
  auditService: AuditService,
  val controllerComponents: MessagesControllerComponents,
  view: TaskListView
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] =
    (identify andThen getData andThen getUprn andThen requireData) { implicit request =>
      request.journeyData match {
        case journeyData if TaskListProgress.canAccessTaskList(journeyData) =>
          Ok(view(TaskListViewModel(journeyData, request.credentialRole)))

        case _ =>
          Redirect(routes.StartController.onPageLoad())
      }
    }

  def continueTo(section: String): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      if (!TaskListProgress.canAccessTaskList(request.journeyData)) {
        Redirect(routes.StartController.onPageLoad())
      } else {
        TaskListRoutes
          .destination(section, request.journeyData, request.credentialRole)
          .fold(Redirect(routes.TaskListController.onPageLoad())) { destination =>
            auditService.auditContinuation(request, destination.sectionName)
            Redirect(destination.call)
          }
      }
    }
}
