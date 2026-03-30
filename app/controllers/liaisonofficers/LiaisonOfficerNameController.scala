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

package controllers.liaisonofficers

import controllers.actions.*
import forms.LiaisonOfficerNameFormProvider
import handlers.ErrorHandler
import models.Mode
import models.journeydata.liaisonofficers.{LiaisonOfficer, LiaisonOfficers}
import navigation.Navigator
import pages.LiaisonOfficerNamePage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.UuidGenerator
import views.html.liaisonofficers.LiaisonOfficerNameView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LiaisonOfficerNameController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  auditContinuation: AuditContinuationAction,
  journeyAnswersService: JourneyAnswersService,
  errorHandler: ErrorHandler,
  formProvider: LiaisonOfficerNameFormProvider,
  navigator: Navigator,
  uuidGenerator: UuidGenerator,
  val controllerComponents: MessagesControllerComponents,
  view: LiaisonOfficerNameView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[String] = formProvider()

  def onPageLoad(id: Option[String], mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen auditContinuation(LiaisonOfficers.sectionName)) {
      implicit request =>

        val preparedFormAndId = (for {
          id              <- id
          liaisonOfficers <- request.journeyData.liaisonOfficers.map(_.liaisonOfficers)
          liaisonOfficer  <- liaisonOfficers.find(_.id == id)
          name            <- liaisonOfficer.fullName
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
            val existingSection = request.journeyData.liaisonOfficers
            val upsertedLO      = LiaisonOfficer(id, Some(answer))
            val updatedSection  = existingSection match {
              case Some(existing) =>
                val existingLOs = existing.liaisonOfficers.filter(_.id != id)
                existing.copy(liaisonOfficers = existingLOs :+ upsertedLO)
              case None           => LiaisonOfficers(Seq(upsertedLO))
            }

            journeyAnswersService
              .update(updatedSection, request.groupId, request.credentials.providerId)
              .map { updatedSection =>
                Redirect(
                  navigator.nextPage(
                    LiaisonOfficerNamePage,
                    updatedSection,
                    mode
                  )
                )
              }
              .recoverWith { case e =>
                logger.warn(
                  s"Failed updating answers for section [${LiaisonOfficers.sectionName}] for groupId [${request.groupId}] with error: [$e]"
                )
                errorHandler.internalServerError
              }
          }
        )
  }
}
