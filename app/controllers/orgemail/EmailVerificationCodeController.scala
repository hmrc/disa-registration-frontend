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
import controllers.orgemail.routes.EmailVerificationCodeController
import forms.EmailVerificationCodeFormProvider
import handlers.ErrorHandler
import models.emailverification.VerifyEmailCodeResult
import models.journeydata.OrganisationEmail
import models.requests.DataRequest
import models.{Mode, ReturnTo}
import navigation.Navigator
import pages.orgemail.OrganisationEmailVerificationCodePage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.orgemail.EmailVerificationCodeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class EmailVerificationCodeController @Inject() (
  override val messagesApi: MessagesApi,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: EmailVerificationCodeFormProvider,
  journeyAnswersService: JourneyAnswersService,
  emailVerificationConnector: EmailVerificationConnector,
  errorHandler: ErrorHandler,
  val controllerComponents: MessagesControllerComponents,
  view: EmailVerificationCodeView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode, returnTo: Option[ReturnTo]): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      organisationEmailOrRedirect(mode, returnTo).fold(
        redirect => redirect,
        email => Ok(view(form, mode, returnTo, email))
      )
    }

  def onSubmit(mode: Mode, returnTo: Option[ReturnTo]): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      organisationEmailOrRedirect(mode, returnTo).fold(
        redirect => Future.successful(redirect),
        email =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, returnTo, email))),
              code =>
                emailVerificationConnector
                  .verifyCode(email, code)
                  .flatMap {
                    case VerifyEmailCodeResult.Verified =>
                      val updatedSection =
                        request.journeyData.organisationEmail
                          .getOrElse(OrganisationEmail())
                          .copy(
                            organisationEmail = Some(email),
                            verified = Some(true)
                          )

                      journeyAnswersService
                        .update(updatedSection, request.groupId, request.credentials.providerId)
                        .map { savedSection =>
                          Redirect(
                            navigator.nextPage(
                              OrganisationEmailVerificationCodePage,
                              savedSection,
                              mode,
                              returnTo
                            )
                          )
                        }

                    case VerifyEmailCodeResult.InvalidCode =>
                      Future.successful(
                        BadRequest(
                          view(
                            form.withError(
                              "value",
                              "emailVerificationCode.error.invalid"
                            ),
                            mode,
                            returnTo,
                            email
                          )
                        )
                      )
                  }
                  .recoverWith { case NonFatal(e) =>
                    logger.warn(
                      s"Failed verifying organisation email confirmation code for groupId [${request.groupId}] with error: [$e]"
                    )
                    errorHandler.internalServerError
                  }
            )
      )
    }

  def requestNewCode(mode: Mode, returnTo: Option[ReturnTo]): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      organisationEmailOrRedirect(mode, returnTo).fold(
        redirect => Future.successful(redirect),
        email =>
          emailVerificationConnector
            .sendCode(email)
            .map { _ =>
              Redirect(EmailVerificationCodeController.onPageLoad(mode, returnTo))
            }
            .recoverWith { case NonFatal(e) =>
              logger.warn(
                s"Failed requesting new organisation email confirmation code for groupId [${request.groupId}] with error: [$e]"
              )
              errorHandler.internalServerError
            }
      )
    }

  private def organisationEmailOrRedirect(mode: Mode, returnTo: Option[ReturnTo])(implicit
    request: DataRequest[_]
  ): Either[Result, String] =
    request.journeyData.organisationEmail
      .flatMap(_.organisationEmail)
      .toRight(Redirect(routes.OrganisationEmailAddressController.onPageLoad(mode, returnTo)))
}
