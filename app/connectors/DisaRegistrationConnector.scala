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
import models.GetOrCreateJourneyData
import models.journeydata.{JourneyData, TaskListSection}
import models.submission.EnrolmentSubmissionResponse
import play.api.Logging
import play.api.http.Status.{CREATED, NOT_FOUND, NO_CONTENT, OK}
import play.api.libs.json.{Json, Writes}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpResponse, StringContextOps, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DisaRegistrationConnector @Inject() (http: HttpClientV2, appConfig: FrontendAppConfig)(implicit
  val ec: ExecutionContext
) extends HttpErrorFunctions
    with Logging {
  def getJourneyData(
    groupId: String
  )(implicit hc: HeaderCarrier): Future[Option[JourneyData]] = {
    val url = s"${appConfig.disaRegistrationBaseUrl}/disa-registration/store/$groupId"
    http
      .get(url"$url")
      .execute[JourneyData]
      .map(Some.apply)
      .recover {
        case e: UpstreamErrorResponse if e.statusCode == NOT_FOUND => None
      }
  }

  def updateTaskListJourney[A <: TaskListSection: Writes](
    data: A,
    groupId: String,
    taskListJourney: String
  )(implicit
    hc: HeaderCarrier
  ): Future[A] = {
    val url = s"${appConfig.disaRegistrationBaseUrl}/disa-registration/store/$groupId/$taskListJourney"
    http
      .post(url"$url")
      .withBody(Json.toJson(data))
      .execute[HttpResponse]
      .flatMap(response =>
        response.status match {
          case s if s == NO_CONTENT => Future.successful(data)
          case status               =>
            logger.error(s"Unexpected status from backend updating journey: [$status] for groupId: [$groupId]")
            Future.failed(
              UpstreamErrorResponse(
                "updateTaskListJourney failed",
                status,
                status,
                response.headers
              )
            )
        }
      )
  }

  def getOrCreateJourneyData(groupId: String)(implicit hc: HeaderCarrier): Future[GetOrCreateJourneyData] = {
    val url = s"${appConfig.disaRegistrationBaseUrl}/disa-registration/$groupId/enrolment"
    http
      .post(url"$url")
      .execute[HttpResponse]
      .map { response =>
        lazy val jd = response.json.as[JourneyData]
        response.status match {
          case OK      => GetOrCreateJourneyData(false, jd)
          case CREATED => GetOrCreateJourneyData(true, jd)
          case status  =>
            throw new UpstreamErrorResponse(
              "getOrCreateJourneyData failed",
              status,
              status,
              response.headers
            )
        }
      }
  }

  def declareAndSubmit(groupId: String)(implicit hc: HeaderCarrier): Future[EnrolmentSubmissionResponse] = {
    val url = s"${appConfig.disaRegistrationBaseUrl}/disa-registration/$groupId/declare-and-submit"
    http
      .post(url"$url")
      .execute[EnrolmentSubmissionResponse]
  }
}
