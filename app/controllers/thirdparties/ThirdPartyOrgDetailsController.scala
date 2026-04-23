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

package controllers.thirdparties

import controllers.actions.*
import controllers.routes.*
import forms.ThirdPartyOrgDetailsFormProvider
import handlers.ErrorHandler
import models.Mode
import models.journeydata.thirdparties.{ThirdPartyOrgDetailsForm, ThirdPartyOrganisations}
import navigation.Navigator
import pages.thirdparties.ThirdPartyOrgDetailsPage
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

class ThirdPartyOrgDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  journeyAnswersService: JourneyAnswersService,
  errorHandler: ErrorHandler,
  formProvider: ThirdPartyOrgDetailsFormProvider,
  navigator: Navigator,
  uuidGenerator: UuidGenerator,
  val controllerComponents: MessagesControllerComponents,
  view: SignatoryNameView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[ThirdPartyOrgDetailsForm] = formProvider()

  def onPageLoad(id: Option[String], mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      val preparedFormAndId = (for {
        id           <- id
        thirdParties <- request.journeyData.thirdPartyOrganisations.map(_.thirdParties)
        thirdParty   <- thirdParties.find(_.id == id)
      } yield {
        val filledForm = form.fill(
          ThirdPartyOrgDetailsForm(
            name = thirdParty.thirdPartyName.getOrElse(""),
            frn = thirdParty.thirdPartyFrn
          )
        )
        (filledForm, id)
      }).getOrElse((form, uuidGenerator.generate()))

      Ok(view(preparedFormAndId._2, preparedFormAndId._1, mode))
    }

  def onSubmit(id: String, mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(id, formWithErrors, mode))),
          answer => {
            val name = answer.name
            val frn  = answer.frn
            request.journeyData.thirdPartyOrganisations match {
              case Some(existing) =>
                val updatedSection =
                  existing.upsertThirdParty(id, name, frn)

                journeyAnswersService
                  .update(updatedSection, request.groupId, request.credentials.providerId)
                  .map { updated =>
                    Redirect(
                      navigator.nextPage(
                        ThirdPartyOrgDetailsPage(id),
                        updated,
                        mode
                      )
                    )
                  }
                  .recoverWith { case NonFatal(e) =>
                    logger.warn(
                      s"Failed updating answers for section [${ThirdPartyOrganisations.sectionName}] for groupId [${request.groupId}] with error: [$e]"
                    )
                    errorHandler.internalServerError
                  }

              case None =>
                Future.successful(Redirect(TaskListController.onPageLoad()))
            }
          }
        )
    }
}
