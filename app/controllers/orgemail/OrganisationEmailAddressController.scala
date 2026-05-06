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

package controllers.orgemail

import connectors.EmailVerificationConnector
import controllers.actions.*
import forms.OrganisationEmailAddressFormProvider
import handlers.ErrorHandler
import models.Mode
import models.journeydata.OrganisationEmail
import navigation.Navigator
import pages.orgemail.OrganisationEmailAddressPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.orgemail.OrganisationEmailAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class OrganisationEmailAddressController @Inject() (
  override val messagesApi: MessagesApi,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  auditContinuation: AuditContinuationAction,
  formProvider: OrganisationEmailAddressFormProvider,
  journeyAnswersService: JourneyAnswersService,
  emailVerificationConnector: EmailVerificationConnector,
  errorHandler: ErrorHandler,
  val controllerComponents: MessagesControllerComponents,
  view: OrganisationEmailAddressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen auditContinuation(OrganisationEmail.sectionName)) {
      implicit request =>
        val preparedForm =
          request.journeyData.organisationEmail
            .flatMap(_.organisationEmail)
            .fold(form)(form.fill)

        Ok(view(preparedForm, mode))
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          email =>
            emailVerificationConnector
              .sendCode(email)
              .flatMap { _ =>
                val updatedSection = OrganisationEmail(
                  organisationEmail = Some(email),
                  verified = Some(false)
                )

                journeyAnswersService
                  .update(updatedSection, request.groupId, request.credentials.providerId)
                  .map { savedSection =>
                    Redirect(
                      navigator.nextPage(
                        OrganisationEmailAddressPage,
                        savedSection,
                        mode
                      )
                    )
                  }
              }
              .recoverWith { case NonFatal(e) =>
                logger.warn(
                  s"Failed submitting organisation email address for groupId [${request.groupId}] with error: [$e]"
                )
                errorHandler.internalServerError
              }
        )
    }
}
