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

package controllers.signatories

import controllers.actions.*
import forms.SignatoryNameFormProvider
import handlers.ErrorHandler
import models.Mode
import models.journeydata.signatories.{Signatories, Signatory}
import navigation.Navigator
import pages.signatories.SignatoryNamePage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.UuidGenerator
import views.html.signatories.SignatoryNameView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class SignatoryNameController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  auditContinuation: AuditContinuationAction,
  journeyAnswersService: JourneyAnswersService,
  errorHandler: ErrorHandler,
  formProvider: SignatoryNameFormProvider,
  navigator: Navigator,
  uuidGenerator: UuidGenerator,
  val controllerComponents: MessagesControllerComponents,
  view: SignatoryNameView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[String] = formProvider()

  def onPageLoad(id: Option[String], mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen auditContinuation(Signatories.sectionName)) {
      implicit request =>

        val preparedFormAndId = (for {
          id          <- id
          signatories <- request.journeyData.signatories.map(_.signatories)
          signatory   <- signatories.find(_.id == id)
          name        <- signatory.fullName
        } yield (form.fill(name), id))
          .getOrElse((form, uuidGenerator.generate()))

        Ok(view(preparedFormAndId._2, preparedFormAndId._1, mode))
    }

  def onSubmit(id: String, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(id, formWithErrors, mode))),
          answer => {
            val updatedSection = request.journeyData.signatories match {
              case Some(existing) =>
                val exists             = existing.signatories.exists(_.id == id)
                val updatedSignatories =
                  if (exists) {
                    existing.signatories.map {
                      case s if s.id == id =>
                        s.copy(fullName = Some(answer))
                      case s               =>
                        s
                    }
                  } else {
                    existing.signatories :+ Signatory(id, Some(answer), jobTitle = None)
                  }
                existing.copy(signatories = updatedSignatories)
              case None           =>
                Signatories(
                  Seq(Signatory(id, Some(answer), jobTitle = None))
                )
            }

            journeyAnswersService
              .update(updatedSection, request.groupId, request.credentials.providerId)
              .map { updatedSection =>
                Redirect(
                  navigator.nextPage(
                    SignatoryNamePage,
                    updatedSection,
                    mode
                  )
                )
              }
              .recoverWith { case NonFatal(e) =>
                logger.warn(
                  s"Failed updating answers for section [${Signatories.sectionName}] for groupId [${request.groupId}] with error: [$e]"
                )
                errorHandler.internalServerError
              }
          }
        )
  }
}
