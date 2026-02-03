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

import config.FrontendAppConfig
import models.grs.{CreateJourneyResponse, GRSResponse, GrsCreateJourneyRequest}
import play.api.Logging
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, StringContextOps, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GrsConnector @Inject() (http: HttpClientV2, appConfig: FrontendAppConfig)(implicit val ec: ExecutionContext)
    extends HttpErrorFunctions
    with Logging {

  def createJourney(
    grsJourneyRequest: GrsCreateJourneyRequest
  )(implicit hc: HeaderCarrier): Future[CreateJourneyResponse] = {

    val url =
      s"${appConfig.incorporatedEntityIdentificationHost}/incorporated-entity-identification/api/limited-company-journey"

    http
      .post(url"$url")
      .withBody(Json.toJson(grsJourneyRequest))
      .setHeader("Content-Type" -> "application/json")
      .execute[CreateJourneyResponse]
      .recoverWith { case errResponse: UpstreamErrorResponse =>
        logger.error(
          s"Create GRS journey failed - Status: ${errResponse.statusCode}, Body: ${errResponse.message}"
        )
        Future.failed(errResponse)
      }
  }

  def fetchJourneyData(journeyId: String)(implicit hc: HeaderCarrier): Future[GRSResponse] = {
    val url =
      s"${appConfig.incorporatedEntityIdentificationHost}/identify-your-incorporated-business/test-only/retrieve-journey?journeyId=$journeyId"
    http
      .get(url"$url")
      .execute[GRSResponse]
      .recoverWith { case errResponse: UpstreamErrorResponse =>
        logger.error(
          s"Fetch GRS journey data failed - Status: ${errResponse.statusCode}, Body: ${errResponse.message}"
        )
        Future.failed(errResponse)
      }
  }
}
