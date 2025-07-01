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

import models.GrsJourneyRequest
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpReads, HttpResponse, InternalServerException, StringContextOps}
import play.api.http.Status._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GRSConnector @Inject() (httpClient: HttpClientV2)(implicit ec: ExecutionContext) {

  def fetchJourneyData(journeyId: String)(implicit hc: HeaderCarrier): Future[String] = {
    val url = s"http://localhost:9718/identify-your-incorporated-business/test-only/retrieve-journey?journeyId=$journeyId"
    httpClient
      .get(url"$url")
      .execute[HttpResponse]
      .map(_.body)
  }

  def createJourney(grsJourneyRequest: GrsJourneyRequest)(implicit hc: HeaderCarrier): Future[String] = {
    println(hc)
    val url = "http://localhost:9718/identify-your-incorporated-business/api/limited-company-journey"
    httpClient.post(url"$url").withBody(Json.toJson(grsJourneyRequest)).setHeader("Content-Type"->"application/json").execute[HttpResponse].map {
      case response@HttpResponse(CREATED, _, _) =>
        (response.json \ "journeyStartUrl").as[String]
      case response => throw new InternalServerException(s"Invalid response from Limited Company: Status: ${response.status} Body: ${response.body}")
    }
  }



}


