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
import controllers.routes.*
import forms.ThirdPartyConnectedOrganisationsFormProvider
import handlers.ErrorHandler
import models.Mode
import models.journeydata.thirdparty.*
import navigation.Navigator
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.thirdparty.ThirdPartyConnectedOrganisationsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class ThirdPartyConnectedOrganisationsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  journeyAnswersService: JourneyAnswersService,
  errorHandler: ErrorHandler,
  formProvider: ThirdPartyConnectedOrganisationsFormProvider,
  navigator: Navigator,
  val controllerComponents: MessagesControllerComponents,
  view: ThirdPartyConnectedOrganisationsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[Set[String]] = formProvider()

  private val NoneValue = "none"

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      val section      = request.journeyData.thirdPartyOrganisations
      val thirdParties = section.map(_.thirdParties).getOrElse(Nil)
      val selected     = section.map(_.connectedOrganisations.toSet).getOrElse(Set.empty)
      val preparedForm = if (selected.isEmpty) form else form.fill(selected)

      Ok(view(thirdParties, preparedForm, mode))
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>

      val sectionOpt   = request.journeyData.thirdPartyOrganisations
      val thirdParties = sectionOpt.map(_.thirdParties).getOrElse(Nil)

      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(thirdParties, formWithErrors, mode))
            ),
          values =>
            sectionOpt match {
              case Some(section) =>
                val updatedConnected =
                  if (values.contains(NoneValue)) Set.empty[String]
                  else values

                val updatedSection =
                  section.copy(
                    connectedOrganisations = updatedConnected
                  )

                journeyAnswersService
                  .update(updatedSection, request.groupId, request.credentials.providerId)
                  .map { _ =>
                    Redirect(TaskListController.onPageLoad())
                  }
                  .recoverWith { case NonFatal(e) =>
                    logger.warn(
                      s"Failed updating answers for section [${ThirdPartyOrganisations.sectionName}] for groupId [${request.groupId}] with error: [$e]"
                    )
                    errorHandler.internalServerError
                  }

              case None =>
                Future.successful(Redirect(IndexController.onPageLoad()))
            }
        )
    }
}
