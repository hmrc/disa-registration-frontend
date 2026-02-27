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

package controllers

import controllers.actions.*
import forms.FcaArticlesFormProvider
import handlers.ErrorHandler
import models.journeydata.CertificatesOfAuthority
import models.{FcaArticles, Mode}
import navigation.Navigator
import pages.FcaArticlesPage
import play.api.data.Form
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.FcaArticlesView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FcaArticlesController @Inject() (
  override val messagesApi: MessagesApi,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  formProvider: FcaArticlesFormProvider,
  journeyAnswersService: JourneyAnswersService,
  errorHandler: ErrorHandler,
  val controllerComponents: MessagesControllerComponents,
  view: FcaArticlesView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Set[FcaArticles]] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData) { implicit request =>
      val preparedForm = (for {
        journeyData             <- request.journeyData
        certificatesOfAuthority <- journeyData.certificatesOfAuthority
        values                  <- certificatesOfAuthority.fcaArticles
      } yield form.fill(values.toSet)).getOrElse(form)

      Ok(view(preparedForm, mode))
    }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
        answer => {
          val existingSection = request.journeyData.flatMap(_.certificatesOfAuthority)
          val updatedSection  =
            existingSection match {
              case Some(existing) =>
                existing.copy(fcaArticles = Some(answer.toSeq))
              case None           => CertificatesOfAuthority(fcaArticles = Some(answer.toSeq))
            }

          journeyAnswersService
            .update(updatedSection, request.groupId, request.credentials.providerId)
            .map { updatedSection =>
              Redirect(
                navigator.nextPage(
                  FcaArticlesPage,
                  updatedSection,
                  mode
                )
              )
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
