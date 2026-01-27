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
import forms.FirmReferenceNumberFormProvider
import handlers.ErrorHandler
import models.Mode
import models.journeydata.OrganisationDetails
import navigation.Navigator
import pages.FirmReferenceNumberPage
import play.api.data.Form
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FormPreparationHelper.prepareForm
import views.html.orgdetails.FirmReferenceNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FirmReferenceNumberController @Inject() (
  override val messagesApi: MessagesApi,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  formProvider: FirmReferenceNumberFormProvider,
  journeyAnswersService: JourneyAnswersService,
  errorHandler: ErrorHandler,
  val controllerComponents: MessagesControllerComponents,
  view: FirmReferenceNumberView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData) { implicit request =>
    val preparedForm = prepareForm(form)(_.organisationDetails.flatMap(_.fcaNumber))(identity)

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
              case Some(existing) => existing.copy(fcaNumber = Some(answer))
              case None           => OrganisationDetails(fcaNumber = Some(answer))
            }
          journeyAnswersService
            .update(updatedSection, request.groupId)
            .map { updatedSection =>
              Redirect(navigator.nextPage(FirmReferenceNumberPage, updatedSection, mode))
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
