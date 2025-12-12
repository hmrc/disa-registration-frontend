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
import models.journeydata.JourneyData
import models.journeydata.isaproducts.IsaProducts
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}

import scala.concurrent.Future

class DisaRegistrationConnectorSpec extends SpecBase {

  trait TestSetup {
    val connector: DisaRegistrationConnector = new DisaRegistrationConnector(mockHttpClient, mockAppConfig)
    val testGroupId: String                  = "123456"
    val testUrl: String                      = "http://localhost:1201"

    when(mockAppConfig.disaRegistrationBaseUrl).thenReturn(testUrl)
    when(mockHttpClient.get(url"$testUrl/disa-registration/store/$testGroupId"))
      .thenReturn(mockRequestBuilder)
    when(mockHttpClient.post(url"$testUrl/disa-registration/store/$testGroupId/${testIsaProductsAnswers.sectionName}"))
      .thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.withBody(any())(any, any, any)).thenReturn(mockRequestBuilder)
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

        val thrown = connector.getJourneyData(testGroupId).failed.futureValue
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
  }
}
