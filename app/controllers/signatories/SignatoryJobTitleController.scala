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
import controllers.routes.*
import forms.SignatoryJobTitleFormProvider
import handlers.ErrorHandler
import models.Mode
import models.journeydata.signatories.{Signatories, Signatory}
import models.requests.DataRequest
import navigation.Navigator
import pages.signatories.SignatoryJobTitlePage
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.signatories.SignatoryJobTitleView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class SignatoryJobTitleController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             journeyAnswersService: JourneyAnswersService,
                                             navigator: Navigator,
                                             identify: IdentifierAction,
                                             errorHandler: ErrorHandler,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             formProvider: SignatoryJobTitleFormProvider,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: SignatoryJobTitleView)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  def onPageLoad(id: String, mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      findSignatoryWithDetails(id).fold {
        Redirect(IndexController.onPageLoad())
      } { case (signatory, name, email) =>
        val preparedForm = email.fold(form)(form.fill)
        Ok(view(id, name, preparedForm, mode))
      }
    }

  def onSubmit(id: String, mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            findSignatoryWithDetails(id).fold {
              Future.successful(Redirect(IndexController.onPageLoad()))
            } { case (_, name, _) =>
              Future.successful(BadRequest(view(id, name, formWithErrors, mode)))
            },
          answer =>
            updatedSectionWithJobTitle(id, answer).fold {
              Future.successful(Redirect(IndexController.onPageLoad()))
            } { updatedSection =>
              journeyAnswersService
                .update(updatedSection, request.groupId, request.credentials.providerId)
                .map { savedSection =>
                  Redirect(
                    navigator.nextPage(
                      SignatoryJobTitlePage,
                      savedSection,
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

  private def findSignatory(id: String)(implicit request: DataRequest[_]): Option[Signatory] =
    request.journeyData.signatories.flatMap(_.signatories.find(_.id == id))

  private def findSignatoryWithDetails(
    id: String
  )(implicit request: DataRequest[_]): Option[(Signatory, String, Option[String])] =
    findSignatory(id).flatMap { signatory =>
      for {
        name <- signatory.fullName
        jobTitle = signatory.jobTitle
      } yield (signatory, name, jobTitle)
    }

  private def updatedSectionWithJobTitle(id: String, jobTitle: String)(implicit
    request: DataRequest[_]
  ): Option[Signatories] =
    request.journeyData.signatories.flatMap { section =>
      val (matching, others) = section.signatories.partition(_.id == id)
      matching.headOption.map { signatory =>
        section.copy(signatories = others :+ signatory.copy(jobTitle = Some(jobTitle)))
      }
    }
}
