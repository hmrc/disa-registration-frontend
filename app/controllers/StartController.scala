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
import forms.InnovativeFinancialProductsFormProvider
import models.journeydata.isaproducts.InnovativeFinancialProduct
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.GrsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StartController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  formProvider: InnovativeFinancialProductsFormProvider,
  val controllerComponents: MessagesControllerComponents,
  genericRegistrationService: GrsService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[Set[InnovativeFinancialProduct]] = formProvider()

  def onPageLoad(): Action[AnyContent] =
    (identify andThen getData).async { implicit request =>
      request.journeyData
        .flatMap(_.businessVerification)
        .flatMap(_.businessVerificationPassed) match {
        case Some(true)  =>
          Future.successful(Redirect(routes.TaskListController.onPageLoad()))
        case Some(false) =>
          // Implement logic to check kick out time from BV failed timestamp?
          // Not sure if we can add ttl to sub objects in mongo doc? probs not?
          Future.successful(Redirect(controllers.routes.BusinessVerificationController.lockout()))
        case _           =>
          genericRegistrationService.getGRSJourneyStartUrl
            .map(url => Redirect(url))
            .recover { case ex =>
              logger.error("Failed to fetch GRS journey URL", ex)
              Redirect(controllers.routes.InternalServerErrorController.onPageLoad())
            }
      }
    }
}
