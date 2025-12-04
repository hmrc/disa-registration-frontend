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

import cats.data.EitherT
import config.FrontendAppConfig
import models.journeyData.TaskListSection
import play.api.libs.json.{Json, Writes}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DisaRegistrationConnector @Inject() (http: HttpClientV2, appConfig: FrontendAppConfig)(implicit
  val ec: ExecutionContext
) extends BaseConnector {
  def getJourneyData(
    groupId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, HttpResponse] = {
    val url = s"${appConfig.disaRegistrationBaseUrl}/disa-registration/store/$groupId"
    read(
      http
        .get(url"$url")
        .execute[Either[UpstreamErrorResponse, HttpResponse]],
      context = "DisaRegistrationConnector: getJourneyData"
    )
  }
  def updateTaskListJourney[A <: TaskListSection: Writes](
    data: A,
    groupId: String,
    taskListJourney: String
  )(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, HttpResponse] = {
    val url = s"${appConfig.disaRegistrationBaseUrl}/disa-registration/store/$groupId/$taskListJourney"
    read(
      http
        .post(url"$url")
        .withBody(Json.toJson(data))
        .execute[Either[UpstreamErrorResponse, HttpResponse]],
      context = "DisaRegistrationConnector: updateTaskListJourney"
    )
  }
}
