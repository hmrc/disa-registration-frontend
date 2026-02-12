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

import base.SpecBase
import config.FrontendAppConfig
import models.journeydata.JourneyData
import models.submission.EnrolmentSubmissionResponse
import models.GetOrCreateJourneyData
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.http.Status.{CREATED, OK}
import play.api.inject
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}

import scala.concurrent.Future

class DisaRegistrationConnectorSpec extends SpecBase {

  trait TestSetup {
    val connector: DisaRegistrationConnector = applicationBuilder(
      None,
      inject.bind[HttpClientV2].toInstance(mockHttpClient),
      inject.bind[FrontendAppConfig].toInstance(mockAppConfig)
    ).build().injector.instanceOf[DisaRegistrationConnector]
    val testUrl: String                      = "http://localhost:1201"

    when(mockAppConfig.disaRegistrationBaseUrl).thenReturn(testUrl)
    when(mockHttpClient.get(url"$testUrl/disa-registration/store/$testGroupId"))
      .thenReturn(mockRequestBuilder)
    when(mockHttpClient.post(url"$testUrl/disa-registration/store/$testGroupId/${testIsaProductsAnswers.sectionName}"))
      .thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.withBody(any())(any, any, any)).thenReturn(mockRequestBuilder)

    when(mockHttpClient.post(url"$testUrl/disa-registration/$testGroupId/declare-and-submit"))
      .thenReturn(mockRequestBuilder)
    when(mockHttpClient.post(url"$testUrl/disa-registration/$testGroupId/enrolment"))
      .thenReturn(mockRequestBuilder)
  }

  "DisaRegistrationConnector" - {

    "getJourneyData" - {

      "return Some(journeyData) on successful call" in new TestSetup {
        when(mockRequestBuilder.execute[JourneyData](any(), any()))
          .thenReturn(Future.successful(testJourneyData))

        val result: Option[JourneyData] = connector.getJourneyData(testGroupId).futureValue

        result shouldBe Some(testJourneyData)
      }

      "return None on a Not Found response" in new TestSetup {
        val upstreamErrorResponse: UpstreamErrorResponse = UpstreamErrorResponse(
          message = "Not found",
          statusCode = 404,
          reportAs = 404,
          headers = Map.empty
        )

        when(mockRequestBuilder.execute[JourneyData](any(), any()))
          .thenReturn(Future.failed(upstreamErrorResponse))

        val result: Option[JourneyData] = connector.getJourneyData(testGroupId).futureValue

        result shouldBe None
      }

      "propagate UpstreamErrorResponse when the call to DISA Registration fails with an UpstreamErrorResponse" in new TestSetup {
        val upstreamErrorResponse: UpstreamErrorResponse = UpstreamErrorResponse(
          message = "Not authorised to access this service",
          statusCode = 401,
          reportAs = 401,
          headers = Map.empty
        )
        when(mockRequestBuilder.execute[JourneyData](any(), any()))
          .thenReturn(Future.failed(upstreamErrorResponse))

        val thrown: Throwable = connector.getJourneyData(testGroupId).failed.futureValue
        thrown mustBe upstreamErrorResponse
      }

      "return UpstreamErrorResponse when the call to DISA Registration fails with an unexpected Throwable exception" in new TestSetup {
        val runtimeException = new RuntimeException("Connection timeout")

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.failed(runtimeException))

        intercept[Throwable] {
          connector.getJourneyData(testGroupId).futureValue
        }
      }
    }

    "getOrCreateJourneyData" - {

      "return GetOrCreateJourneyData on CREATED" in new TestSetup {
        val httpResponse    = HttpResponse(
          status = CREATED,
          body = Json.toJson(testJourneyData).toString
        )
        val expectedOutcome = GetOrCreateJourneyData(
          isNewEnrolmentJourney = true,
          journeyData = testJourneyData
        )

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(httpResponse))

        val result = connector.getOrCreateJourneyData(testGroupId).futureValue

        result shouldBe expectedOutcome
      }

      "return GetOrCreateJourneyData on OK" in new TestSetup {
        val httpResponse    = HttpResponse(
          status = OK,
          body = Json.toJson(testJourneyData).toString
        )
        val expectedOutcome = GetOrCreateJourneyData(
          isNewEnrolmentJourney = false,
          journeyData = testJourneyData
        )

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(httpResponse))

        val result = connector.getOrCreateJourneyData(testGroupId).futureValue

        result shouldBe expectedOutcome
      }

      "propagate UpstreamErrorResponse when the call to DISA Registration fails with an UpstreamErrorResponse" in new TestSetup {
        val upstreamErrorResponse: UpstreamErrorResponse = UpstreamErrorResponse(
          message = "Not authorised to access this service",
          statusCode = 401,
          reportAs = 401,
          headers = Map.empty
        )

        when(mockRequestBuilder.execute[GetOrCreateJourneyData](any(), any()))
          .thenReturn(Future.failed(upstreamErrorResponse))

        val thrown = connector.getOrCreateJourneyData(testGroupId).failed.futureValue
        thrown shouldBe upstreamErrorResponse
      }

      "propagate Throwable when the call fails with an unexpected exception" in new TestSetup {
        val runtimeException = new RuntimeException("Connection timeout")

        when(mockRequestBuilder.execute[GetOrCreateJourneyData](any(), any()))
          .thenReturn(Future.failed(runtimeException))

        val thrown = connector.getOrCreateJourneyData(testGroupId).failed.futureValue
        thrown shouldBe runtimeException
      }
    }

    "updateTaskListJourney" - {

      "return Unit on successful call" in new TestSetup {
        val mockHttpResponse: HttpResponse = HttpResponse(
          status = 204,
          headers = Map.empty
        )

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(mockHttpResponse))

        val result: Unit = connector
          .updateTaskListJourney(testIsaProductsAnswers, testGroupId, testIsaProductsAnswers.sectionName)
          .futureValue

        result shouldBe ()
      }

      "return UpstreamErrorResponse when the call to DISA Registration fails with an UpstreamErrorResponse" in new TestSetup {
        val upstreamErrorResponse: UpstreamErrorResponse = UpstreamErrorResponse(
          message = "Not authorised to access this service",
          statusCode = 401,
          reportAs = 401,
          headers = Map.empty
        )
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.failed(upstreamErrorResponse))

        intercept[Throwable] {
          connector
            .updateTaskListJourney(testIsaProductsAnswers, testGroupId, testIsaProductsAnswers.sectionName)
            .futureValue
        }
      }

      "return UpstreamErrorResponse when the call to DISA Registration fails with an unexpected Throwable exception" in new TestSetup {
        val runtimeException = new RuntimeException("Connection timeout")

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.failed(runtimeException))

        intercept[Throwable] {
          connector
            .updateTaskListJourney(testIsaProductsAnswers, testGroupId, testIsaProductsAnswers.sectionName)
            .futureValue
        }
      }
    }

    "declareAndSubmit" - {

      "return EnrolmentSubmissionResponse on successful call" in new TestSetup {
        val response = EnrolmentSubmissionResponse(testString)

        when(mockRequestBuilder.execute[EnrolmentSubmissionResponse](any(), any()))
          .thenReturn(Future.successful(response))

        val result = connector.declareAndSubmit(testGroupId).futureValue

        result shouldBe response
      }

      "propagate UpstreamErrorResponse when the call to DISA Registration fails with an UpstreamErrorResponse" in new TestSetup {
        val upstreamErrorResponse: UpstreamErrorResponse = UpstreamErrorResponse(
          message = "Not authorised to access this service",
          statusCode = 401,
          reportAs = 401,
          headers = Map.empty
        )

        when(mockRequestBuilder.execute[EnrolmentSubmissionResponse](any(), any()))
          .thenReturn(Future.failed(upstreamErrorResponse))

        val thrown = connector.declareAndSubmit(testGroupId).failed.futureValue
        thrown shouldBe upstreamErrorResponse
      }

      "propagate Throwable when the call to DISA Registration fails with an unexpected exception" in new TestSetup {
        val runtimeException = new RuntimeException("Connection timeout")

        when(mockRequestBuilder.execute[EnrolmentSubmissionResponse](any(), any()))
          .thenReturn(Future.failed(runtimeException))

        val thrown = connector.declareAndSubmit(testGroupId).failed.futureValue
        thrown shouldBe runtimeException
      }
    }
  }
}
