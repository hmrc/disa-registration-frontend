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

package services

import config.FrontendAppConfig
import connectors.GrsConnector
import models.grs.{GRSResponse, GrsCreateJourneyRequest, Labels, ServiceLabel}
import play.api.Logging
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GrsService @Inject() (grsConnector: GrsConnector, appConfig: FrontendAppConfig, messagesApi: MessagesApi)(implicit
  val ec: ExecutionContext
) extends HttpErrorFunctions
    with Logging {

  def getGRSJourneyStartUrl(implicit hc: HeaderCarrier, request: RequestHeader): Future[String] = {
    val serviceLabel: String = messagesApi.preferred(Seq(Lang("en"))).messages("service.name")

    val requestBody = GrsCreateJourneyRequest(
      continueUrl = appConfig.grsCallback,
      businessVerificationCheck = true,
      deskProServiceId = "deskProServiceId",
      signOutUrl = appConfig.host + controllers.auth.routes.SignedOutController.signOutAnswersNotSaved().url,
      regime =
        "ISA", // TODO: waiting on confirmation for this only options according to docs are VATC/PPT but seems to work fine with ISA locally
      accessibilityUrl = appConfig.accessibilityStatementUrl,
      labels = Some(Labels(en = Some(ServiceLabel(serviceLabel))))
    )

    grsConnector.createJourney(grsJourneyRequest = requestBody).map(_.journeyStartUrl)
  }

  def fetchGRSJourneyData(journeyId: String)(implicit hc: HeaderCarrier): Future[GRSResponse] =
    grsConnector.fetchJourneyData(journeyId)
}
