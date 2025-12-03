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
import forms.IsaProductsFormProvider
import handlers.ErrorHandler
import models.Mode
import models.journeyData.isaProducts.IsaProduct
import navigation.Navigator
import pages.IsaProductsPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.IsaProductsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IsaProductsController @Inject() (
  override val messagesApi: MessagesApi,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: IsaProductsFormProvider,
  journeyAnswersService: JourneyAnswersService,
  errorHandler: ErrorHandler,
  val controllerComponents: MessagesControllerComponents,
  view: IsaProductsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.journeyData.isaProducts.fold(form)(_.isaProducts match {
      case None              => form
      case Some(isaProducts) => form.fill(isaProducts.toSet)
    })

    Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          answer => {
            val data           = request.journeyData
            val updatedSection = data.isaProducts.map(_.copy(isaProducts = Some(answer.toSeq)))

            updatedSection.fold {
              errorHandler.badRequestError
            } { section =>
              journeyAnswersService.update(section, request.groupId).map { _ =>
                Redirect(navigator.nextPage(IsaProductsPage, mode))
              }
            }
          }
        )
  }
}
