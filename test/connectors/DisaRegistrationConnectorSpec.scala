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
import cats.data.EitherT
import models.journeyData.isaProducts.IsaProducts
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import play.api.libs.json.Json
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

      "return HttpResponse on successful call" in new TestSetup {
        val mockHttpResponse: HttpResponse = HttpResponse(
          status = 200,
          json = Json.toJson(testIsaProductsAnswers),
          headers = Map.empty
        )

        when(mockBaseConnector.read(any(), any()))
          .thenAnswer { invocation =>
            val future = invocation
              .getArgument[Future[Either[UpstreamErrorResponse, HttpResponse]]](
                0,
                classOf[Future[Either[UpstreamErrorResponse, HttpResponse]]]
              )
            EitherT(future)
          }

        when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
          .thenReturn(Future.successful(Right(mockHttpResponse)))

        val result: Either[UpstreamErrorResponse, HttpResponse] =
          connector.getJourneyData(testGroupId).value.futureValue

        result shouldBe Right(mockHttpResponse)
      }

      "return UpstreamErrorResponse when the call to DISA Registration fails with an UpstreamErrorResponse" in new TestSetup {
        val upstreamErrorResponse: UpstreamErrorResponse = UpstreamErrorResponse(
          message = "Not authorised to access this service",
          statusCode = 401,
          reportAs = 401,
          headers = Map.empty
        )
        when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
          .thenReturn(Future.successful(Left(upstreamErrorResponse)))

        val result: Either[UpstreamErrorResponse, HttpResponse] =
          connector.getJourneyData(testGroupId).value.futureValue

        result shouldBe Left(upstreamErrorResponse)
      }

      "return UpstreamErrorResponse when the call to DISA Registration fails with an unexpected Throwable exception" in new TestSetup {
        val runtimeException = new RuntimeException("Connection timeout")

        when(mockBaseConnector.read(any(), any()))
          .thenAnswer { invocation =>
            val future = invocation
              .getArgument[Future[Either[UpstreamErrorResponse, HttpResponse]]](
                0,
                classOf[Future[Either[UpstreamErrorResponse, HttpResponse]]]
              )
            EitherT(
              future.recover { case e =>
                Left(UpstreamErrorResponse(s"Unexpected error: ${e.getMessage}", 500, 500))
              }
            )
          }
        when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
          .thenReturn(Future.failed(runtimeException))

        val Left(result): Either[UpstreamErrorResponse, HttpResponse] =
          connector.getJourneyData(testGroupId).value.futureValue

        result.statusCode shouldBe 500
        result.message      should include("Unexpected error: Connection timeout")
      }
    }

    "updateTaskListJourney" - {

      "return HttpResponse on successful call" in new TestSetup {
        val mockHttpResponse: HttpResponse = HttpResponse(
          status = 204,
          headers = Map.empty
        )

        when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
          .thenReturn(Future.successful(Right(mockHttpResponse)))

        val result: Either[UpstreamErrorResponse, HttpResponse] = connector
          .updateTaskListJourney(testIsaProductsAnswers, testGroupId, testIsaProductsAnswers.sectionName)
          .value
          .futureValue

        result shouldBe Right(mockHttpResponse)
      }

      "return UpstreamErrorResponse when the call to DISA Registration fails with an UpstreamErrorResponse" in new TestSetup {
        val upstreamErrorResponse: UpstreamErrorResponse = UpstreamErrorResponse(
          message = "Not authorised to access this service",
          statusCode = 401,
          reportAs = 401,
          headers = Map.empty
        )
        when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
          .thenReturn(Future.successful(Left(upstreamErrorResponse)))

        val result: Either[UpstreamErrorResponse, HttpResponse] = connector
          .updateTaskListJourney(testIsaProductsAnswers, testGroupId, testIsaProductsAnswers.sectionName)
          .value
          .futureValue

        result shouldBe Left(upstreamErrorResponse)
      }

      "return UpstreamErrorResponse when the call to DISA Registration fails with an unexpected Throwable exception" in new TestSetup {
        val runtimeException = new RuntimeException("Connection timeout")

        when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
          .thenReturn(Future.failed(runtimeException))

        val Left(result): Either[UpstreamErrorResponse, HttpResponse] =
          connector
            .updateTaskListJourney(testIsaProductsAnswers, testGroupId, testIsaProductsAnswers.sectionName)
            .value
            .futureValue

        result.statusCode shouldBe 500
        result.message      should include("Unexpected error: Connection timeout")
      }
    }
  }
}
