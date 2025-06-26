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

package controllers

import controllers.actions.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.StartView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StartController @Inject() (
  override val messagesApi: MessagesApi,
  identify:                 IdentifierAction,
  val controllerComponents: MessagesControllerComponents,
  view:                     StartView,
  grs:                      GRSConnector,
  val authConnector:        AuthConnector
)(implicit ec:              ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with AuthorisedFunctions {

  def onPageLoad: Action[AnyContent] = identify { implicit request =>
    Ok(view())
  }

  def grsJourney: Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      Future.successful(Redirect("http://localhost:9718/identify-your-incorporated-business/test-only/create-limited-company-journey"))
    }.recover { case ex: Throwable =>
      InternalServerError(s"Failed to create journey: ${ex.getMessage}")
    }
  }

  def retrieveData(journeyId: String)(): Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      grs
        .fetchJourneyData(journeyId)
        .map { resultString =>
          Ok(resultString)
        }
        .recover { case ex: Exception =>
          InternalServerError(s"Failed to fetch GRS journey data: ${ex.getMessage}")
        }
    }
  }

  def retrieveData2(): Action[AnyContent] = Action.async { implicit request =>
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised() {
      request.getQueryString("journeyId") match {
        case Some(journeyId) =>
          grs.fetchJourneyData(journeyId).map { resultString =>
            Ok(resultString)
          }.recover {
            case ex: Exception =>
              InternalServerError(s"Failed to fetch GRS journey data: ${ex.getMessage}")
          }

        case None =>
          Future.successful(BadRequest("Missing 'journeyId' query parameter"))
      }
    }
  }

  def retrieveData3(): Action[AnyContent] = Action.async { implicit request =>
    request.getQueryString("journeyId") match {
      case Some(journeyId) =>
        grs
          .fetchJourneyData(journeyId)
          .map { resultString =>
            Ok(resultString)
          }
          .recover { case ex: Exception =>
            InternalServerError(s"Failed to fetch GRS journey data: ${ex.getMessage}")
          }

      case None =>
        Future.successful(BadRequest("Missing 'journeyId' query parameter"))
    }
  }

}
