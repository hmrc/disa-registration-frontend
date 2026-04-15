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
import controllers.routes.TaskListController
import controllers.actions.*
import forms.LiaisonOfficerNameFormProvider
import handlers.ErrorHandler
import models.Mode
import models.journeydata.liaisonofficers.{LiaisonOfficer, LiaisonOfficers}
import models.requests.DataRequest
import navigation.Navigator
import pages.liaisonofficers.LiaisonOfficerNamePage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.UuidGenerator
import views.html.liaisonofficers.LiaisonOfficerNameView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class LiaisonOfficerNameController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  auditContinuation: AuditContinuationAction,
  journeyAnswersService: JourneyAnswersService,
  errorHandler: ErrorHandler,
  formProvider: LiaisonOfficerNameFormProvider,
  navigator: Navigator,
  uuidGenerator: UuidGenerator,
  appConfig: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents,
  view: LiaisonOfficerNameView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[String] = formProvider()

  def onPageLoad(id: Option[String], mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen auditContinuation(LiaisonOfficers.sectionName)) {
      implicit request =>

        lazy val (liaisonOfficerId, preparedForm) =
          (for {
            existingId <- id
            officers   <- request.journeyData.liaisonOfficers.map(_.liaisonOfficers)
            officer    <- officers.find(_.id == existingId)
            name       <- officer.fullName
          } yield (existingId, form.fill(name)))
            .getOrElse((uuidGenerator.generate(), form))

        if (canAddAnother(appConfig)) Ok(view(liaisonOfficerId, preparedForm, mode))
        else Redirect(TaskListController.onPageLoad())
    }

  def onSubmit(id: String, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(id, formWithErrors, mode))),
          answer => {
            val updatedSection =
              request.journeyData.liaisonOfficers
                .getOrElse(LiaisonOfficers(Nil))
                .upsertLiaisonOfficer(id, answer, appConfig.maxLiaisonOfficers)

            updatedSection match {
              case None =>
                logger.info(s"Addition of liaison officer blocked by limit for groupId [${request.groupId}]")
                Future.successful(Redirect(TaskListController.onPageLoad()))
              case Some(updatedSection) =>
                journeyAnswersService
                  .update(updatedSection, request.groupId, request.credentials.providerId)
                  .map { updatedSection =>
                    Redirect(
                      navigator.nextPage(
                        LiaisonOfficerNamePage(id),
                        updatedSection,
                        mode
                      )
                    )
                  }
                  .recoverWith { case NonFatal(e) =>
                    logger.warn(
                      s"Failed updating answers for section [${LiaisonOfficers.sectionName}] for groupId [${request.groupId}] with error: [$e]"
                    )
                    errorHandler.internalServerError
                  }
            }
          }
        )
  }

  private def canAddAnother(appConfig: FrontendAppConfig)(implicit request: DataRequest[_])   = request.journeyData.liaisonOfficers.forall(_.canAddAnother(appConfig.maxLiaisonOfficers))
}
