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

import config.FrontendAppConfig
import controllers.actions.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.{HeaderCarrier, RequestId}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.{EndView, StartView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StartController @Inject() (
  override val messagesApi: MessagesApi,
  identify:                 IdentifierAction,
  val controllerComponents: MessagesControllerComponents,
  view:                     StartView,
  endView:                  EndView,
  grs:                      GRSConnector,
  val authConnector:        AuthConnector,
  config:                   FrontendAppConfig
)(implicit ec:              ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with AuthorisedFunctions {

  def onPageLoad: Action[AnyContent] = identify { implicit request =>
    Ok(view())
  }

  def grsJourney: Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      Future.successful(Redirect(config.grsJourneyUrl))
    }.recover { case ex: Throwable =>
      InternalServerError(s"Failed to create journey: ${ex.getMessage}")
    }
  }

  def retrieveData(journeyId: String): Action[AnyContent] = identify.async { implicit request =>
    val headers: Seq[(String, String)] = request.headers.toSimpleMap.toSeq.filterNot { case (key, _) =>
      key.equalsIgnoreCase("Authorization")
    }

    val hc = HeaderCarrier(
      extraHeaders = headers,
      authorization = None,
      otherHeaders = Seq.empty,
      sessionId = None,
      requestId = request.headers.get("X-Request-ID").map(RequestId)
    )

    authorised() {
      grs
        .fetchJourneyData(journeyId)(hc)
        .map { resultString =>
          Ok(endView(resultString))
        }
        .recover { case ex: Exception =>
          InternalServerError(s"Failed to fetch GRS journey data: ${ex.getMessage}")
        }

    }
  }
}
