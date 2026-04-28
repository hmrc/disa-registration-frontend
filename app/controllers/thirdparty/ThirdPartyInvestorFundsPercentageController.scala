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

package controllers.thirdparty

import controllers.actions.*
import controllers.routes.*
import forms.ThirdPartyInvestorFundsPercentageFormProvider
import handlers.ErrorHandler
import models.journeydata.thirdparty.{ThirdParty, ThirdPartyOrganisations}
import models.requests.DataRequest
import models.{Mode, YesNoAnswer}
import navigation.Navigator
import pages.thirdparty.ThirdPartyInvestorFundsPercentagePage
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.thirdparty.ThirdPartyInvestorFundsPercentageView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class ThirdPartyInvestorFundsPercentageController @Inject() (
  override val messagesApi: MessagesApi,
  journeyAnswersService: JourneyAnswersService,
  navigator: Navigator,
  identify: IdentifierAction,
  errorHandler: ErrorHandler,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: ThirdPartyInvestorFundsPercentageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: ThirdPartyInvestorFundsPercentageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  def onPageLoad(id: String, mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      findThirdPartyWithDetails(id).fold {
        Redirect(IndexController.onPageLoad())
      } { case (thirdParty, name, answer) =>
        val preparedForm = answer.fold(form)(form.fill)
        Ok(view(id, name, preparedForm, mode))
      }
    }

  def onSubmit(id: String, mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            findThirdPartyWithDetails(id).fold {
              Future.successful(Redirect(IndexController.onPageLoad()))
            } { case (_, name, _) =>
              Future.successful(BadRequest(view(id, name, formWithErrors, mode)))
            },
          answer =>
            updatedSectionWithAnswer(id, answer).fold {
              Future.successful(Redirect(IndexController.onPageLoad()))
            } { updatedSection =>
              journeyAnswersService
                .update(updatedSection, request.groupId, request.credentials.providerId)
                .map { savedSection =>
                  Redirect(
                    navigator.nextPage(
                      ThirdPartyInvestorFundsPercentagePage(id),
                      savedSection,
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
            }
        )
    }

  private def findThirdParty(id: String)(implicit request: DataRequest[_]): Option[ThirdParty] =
    request.journeyData.thirdPartyOrganisations.flatMap(_.thirdParties.find(_.id == id))

  private def findThirdPartyWithDetails(
    id: String
  )(implicit request: DataRequest[_]): Option[(ThirdParty, String, Option[String])] =
    findThirdParty(id).flatMap { thirdParty =>
      for {
        name                   <- thirdParty.thirdPartyName
        investorFundsPercentage = thirdParty.investorFundsPercentage
      } yield (thirdParty, name, investorFundsPercentage)
    }

  private def updatedSectionWithAnswer(id: String, answer: String)(implicit
    request: DataRequest[_]
  ): Option[ThirdPartyOrganisations] =
    request.journeyData.thirdPartyOrganisations.flatMap { section =>
      val (matching, others) = section.thirdParties.partition(_.id == id)
      matching.headOption.map { thirdParty =>
        section.copy(thirdParties = others :+ thirdParty.copy(investorFundsPercentage = Some(answer)))
      }
    }
}
