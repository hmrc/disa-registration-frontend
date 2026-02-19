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

package controllers.isaproducts

import controllers.actions.*
import forms.PeerToPeerPlatformFormProvider
import handlers.ErrorHandler
import models.Mode
import models.journeydata.isaproducts.IsaProducts
import navigation.Navigator
import pages.PeerToPeerPlatformPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.isaproducts.PeerToPeerPlatformView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PeerToPeerPlatformController @Inject() (
  override val messagesApi: MessagesApi,
  journeyAnswersService: JourneyAnswersService,
  navigator: Navigator,
  errorHandler: ErrorHandler,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  formProvider: PeerToPeerPlatformFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: PeerToPeerPlatformView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData) { implicit request =>
    val preparedForm = (for {
      journeyData <- request.journeyData
      section     <- journeyData.isaProducts
      name        <- section.p2pPlatform
    } yield form.fill(name)).getOrElse(form)

    Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
        answer => {
          val updatedSection =
            request.journeyData.flatMap(_.isaProducts) match {
              case Some(existing) => existing.copy(p2pPlatform = Some(answer))
              case None           => IsaProducts(p2pPlatform = Some(answer))
            }

          journeyAnswersService
            .update(updatedSection, request.groupId, request.credentials.providerId)
            .map { updatedSection =>
              Redirect(navigator.nextPage(PeerToPeerPlatformPage, updatedSection, mode))
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
