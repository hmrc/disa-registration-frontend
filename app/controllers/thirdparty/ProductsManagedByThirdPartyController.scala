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
import forms.YesNoAnswerFormProvider
import handlers.ErrorHandler
import handlers.JourneyHandler.clearStalePages
import models.Mode
import models.journeydata.thirdparty.ThirdPartyOrganisations
import navigation.Navigator
import pages.thirdparty.ProductsManagedByThirdPartyPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.thirdparty.ProductsManagedByThirdPartyView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class ProductsManagedByThirdPartyController @Inject()(
                                                       override val messagesApi: MessagesApi,
                                                       navigator: Navigator,
                                                       identify: IdentifierAction,
                                                       getData: DataRetrievalAction,
                                                       requireData: DataRequiredAction,
                                                       formProvider: YesNoAnswerFormProvider,
                                                       errorHandler: ErrorHandler,
                                                       journeyAnswersService: JourneyAnswersService,
                                                       val controllerComponents: MessagesControllerComponents,
                                                       view: ProductsManagedByThirdPartyView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  val form = formProvider("productsManagedByThirdParty.error.required")

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.journeyData.thirdPartyOrganisations.flatMap(_.managedByThirdParty) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        answer => {
            val existingSection = request.journeyData.thirdPartyOrganisations
            val updatedSection  =
              existingSection match {
                case Some(existing) =>
                  clearStalePages(ProductsManagedByThirdPartyPage, existing.copy(managedByThirdParty = Some(answer)))
                case _           => ThirdPartyOrganisations(managedByThirdParty = Some(answer))
              }

            journeyAnswersService
              .update(updatedSection, request.groupId, request.credentials.providerId)
              .map { updatedSection =>
                Redirect(
                  navigator.nextPage(
                    ProductsManagedByThirdPartyPage,
                    existingSection,
                    updatedSection,
                    mode
                  )
                )
              }
              .recoverWith { case NonFatal(e) =>
                logger.warn(
                  s"Failed updating answers for section [${updatedSection.sectionName}] for groupId [${request.groupId}] with error: [$e]"
                )
                errorHandler.internalServerError
              }
          }
      )
  }
}
