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
import forms.YesNoAnswerFormProvider
import handlers.ErrorHandler
import handlers.JourneyHandler.clearStalePages
import models.journeydata.OrganisationDetails
import models.{Mode, ReturnTo, YesNoAnswer}
import navigation.Navigator
import pages.organisationdetails.TradingUsingDifferentNamePage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.orgdetails.TradingUsingDifferentNameView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class TradingUsingDifferentNameController @Inject() (
  override val messagesApi: MessagesApi,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: YesNoAnswerFormProvider,
  journeyAnswersService: JourneyAnswersService,
  errorHandler: ErrorHandler,
  val controllerComponents: MessagesControllerComponents,
  view: TradingUsingDifferentNameView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[YesNoAnswer] = formProvider("tradingUsingDifferentName.error.required")

  def onPageLoad(mode: Mode, returnTo: Option[ReturnTo]): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.journeyData.organisationDetails
        .flatMap(_.tradingUsingDifferentName)
        .fold(form)(form.fill)

      Ok(view(preparedForm, mode, returnTo))
    }

  def onSubmit(mode: Mode, returnTo: Option[ReturnTo]): Action[AnyContent] = (identify andThen getData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, returnTo))),
          answer => {
            val existing: Option[OrganisationDetails] =
              request.journeyData.flatMap(_.organisationDetails)
            val updatedSection                        = existing match {
              case Some(existing) =>
                clearStalePages(TradingUsingDifferentNamePage, existing.copy(tradingUsingDifferentName = Some(answer)))
              case None           => OrganisationDetails(tradingUsingDifferentName = Some(answer))
            }
            journeyAnswersService
              .update(updatedSection, request.groupId, request.credentials.providerId)
              .map { updatedSection =>
                Redirect(navigator.nextPage(TradingUsingDifferentNamePage, existing, updatedSection, mode, returnTo))
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
