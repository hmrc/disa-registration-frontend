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

package connectors

import models.grs.{GRSResponse, GrsCreateJourneyRequest}
import play.api.Logging
import play.api.mvc.Request
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GenericRegistrationService @Inject()(genericRegistrationServiceConnector: GenericRegistrationServiceConnector)
                                          (implicit val ec: ExecutionContext, headerCarrier: HeaderCarrier) extends HttpErrorFunctions with Logging {

  def getGRSJourneyStartUrl: Future[String] = {
   
    val requestBody = GrsCreateJourneyRequest(
      continueUrl = "???",
      businessVerificationCheck = true,
      optServiceName = Some("Register to Manage ISAs"),
      deskProServiceId = "deskProServiceId",
      signOutUrl = "???",
      regime = "???",
      accessibilityUrl = "???",
      labels = None)

    genericRegistrationServiceConnector.createJourney(grsJourneyRequest = requestBody).map(_.journeyStartUrl)
  }

  def fetchGRSJourneyData(journeyId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[GRSResponse] = {
    genericRegistrationServiceConnector.fetchJourneyData(journeyId)
  }
}


