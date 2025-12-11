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

import controllers.actions.{DataRetrievalAction, IdentifierAction}
import forms.ZReferenceNumberFormProvider
import handlers.ErrorHandler
import models.Mode
import models.journeyData.OrganisationDetails
import models.journeyData.isaProducts.IsaProducts
import navigation.Navigator
import pages.IsaProductsPage
import play.api.i18n.I18nSupport
import play.api.i18n.Lang.logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.orgdetails.ZReferenceNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ZReferenceNumberController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  formProvider: ZReferenceNumberFormProvider,
  view: ZReferenceNumberView,
  journeyAnswersService: JourneyAnswersService,
  navigator: Navigator,
  errorHandler: ErrorHandler)(implicit executionContext: ExecutionContext) extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    val preparedForm = (for {
      journeyData <- request.journeyData
      orgDetails <- journeyData.organisationDetails
      values <- orgDetails.zRefNumber
    } yield form.fill(values)).getOrElse(form)
    Future.successful(Ok(view(preparedForm, mode)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
        answer =>
        {
          val updatedSection =
          request.journeyData.flatMap(_.organisationDetails) match {
            case Some(existing) => existing.copy(zRefNumber = Some(answer))
            case None => OrganisationDetails(zRefNumber = Some(answer))
          }

          journeyAnswersService
            .update(updatedSection, request.groupId)
            .map { _ =>
              Redirect(navigator.nextPage(IsaProductsPage, mode))
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
