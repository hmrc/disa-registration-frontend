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
import controllers.routes.IndexController
import forms.TelephoneNumberFormProvider
import handlers.ErrorHandler
import models.Mode
import models.journeydata.liaisonofficers.{LiaisonOfficer, LiaisonOfficers}
import models.requests.DataRequest
import navigation.Navigator
import pages.liaisonofficers.LiaisonOfficerPhoneNumberPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.liaisonofficers.LiaisonOfficerPhoneNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class LiaisonOfficerPhoneNumberController @Inject() (
  override val messagesApi: MessagesApi,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: TelephoneNumberFormProvider,
  journeyAnswersService: JourneyAnswersService,
  errorHandler: ErrorHandler,
  val controllerComponents: MessagesControllerComponents,
  view: LiaisonOfficerPhoneNumberView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form = formProvider("liaisonOfficerPhoneNumber")

  def onPageLoad(id: String, mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      findLiaisonOfficerWithDetails(id).fold {
        Redirect(IndexController.onPageLoad())
      } { case (liaisonOfficer, name, number) =>
        val preparedForm = number.fold(form)(form.fill)
        Ok(view(id, name, preparedForm, mode))
      }
    }

  def onSubmit(id: String, mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            findLiaisonOfficerWithDetails(id).fold {
              Future.successful(Redirect(IndexController.onPageLoad()))
            } { case (_, name, _) =>
              Future.successful(BadRequest(view(id, name, formWithErrors, mode)))
            },
          answer =>
            updatedSectionWithPhoneNumber(id, answer).fold {
              Future.successful(Redirect(IndexController.onPageLoad()))
            } { updatedSection =>
              journeyAnswersService
                .update(updatedSection, request.groupId, request.credentials.providerId)
                .map { savedSection =>
                  Redirect(
                    navigator.nextPage(
                      LiaisonOfficerPhoneNumberPage,
                      savedSection,
                      mode
                    )
                  )
                }
                .recoverWith { case NonFatal(e) =>
                  logger.warn(
                    s"Failed updating answers for section [${LiaisonOfficers.sectionName}] for groupId [${request.groupId}] with error: [$e]"
                  )
                  errorHandler.internalServerError
                }
            }
        )
    }

  private def findLiaisonOfficer(id: String)(implicit request: DataRequest[_]): Option[LiaisonOfficer] =
    request.journeyData.liaisonOfficers.flatMap(_.liaisonOfficers.find(_.id == id))

  private def findLiaisonOfficerWithDetails(
    id: String
  )(implicit request: DataRequest[_]): Option[(LiaisonOfficer, String, Option[String])] =
    findLiaisonOfficer(id).flatMap { liaisonOfficer =>
      for {
        name  <- liaisonOfficer.fullName
        number = liaisonOfficer.phoneNumber
      } yield (liaisonOfficer, name, number)
    }

  private def updatedSectionWithPhoneNumber(id: String, phoneNumber: String)(implicit
    request: DataRequest[_]
  ): Option[LiaisonOfficers] =
    request.journeyData.liaisonOfficers.flatMap { section =>
      val (matching, others) = section.liaisonOfficers.partition(_.id == id)

      matching.headOption.map { liaisonOfficer =>
        section.copy(liaisonOfficers = others :+ liaisonOfficer.copy(phoneNumber = Some(phoneNumber)))
      }
    }
}
