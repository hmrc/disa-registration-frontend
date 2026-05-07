/*
 * Copyright 2026 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SERVICE_UNAVAILABLE}
import play.api.inject
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.Future

class EmailVerificationConnectorSpec extends SpecBase {

  trait TestSetup {

    val connector: EmailVerificationConnector = applicationBuilder(
      None,
      inject.bind[HttpClientV2].toInstance(mockHttpClient),
      inject.bind[FrontendAppConfig].toInstance(mockAppConfig)
    ).build().injector.instanceOf[EmailVerificationConnector]

    val testUrl: String = "http://localhost:9898"

    when(mockAppConfig.emailVerificationBaseUrl).thenReturn(testUrl)

    when(mockHttpClient.post(any())(any()))
      .thenReturn(mockRequestBuilder)

    when(mockRequestBuilder.withBody(any())(any(), any(), any()))
      .thenReturn(mockRequestBuilder)
  }

  "EmailVerificationConnector" - {

    "sendCode" - {

      "must return Unit when downstream returns OK" in new TestSetup {

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        val result = connector
          .sendCode("test@example.com")
          .futureValue

        result shouldBe ()
      }

      "must include request body" in new TestSetup {

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        connector
          .sendCode("test@example.com")
          .futureValue

        verify(mockRequestBuilder).withBody(any())(any(), any(), any())
      }

      "must fail with UpstreamErrorResponse when downstream returns BAD_REQUEST" in new TestSetup {

        val upstreamBody = """{"code":"INVALID_REQUEST"}"""

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, upstreamBody)))

        val thrown = connector
          .sendCode("test@example.com")
          .failed
          .futureValue

        thrown mustBe a[UpstreamErrorResponse]

        val upstreamError = thrown.asInstanceOf[UpstreamErrorResponse]

        upstreamError.statusCode mustEqual BAD_REQUEST
        upstreamError.reportAs mustEqual INTERNAL_SERVER_ERROR
        upstreamError.getMessage must include("Email verification call [POST /v2/send-code] failed")
        upstreamError.getMessage must include(s"upstream status [$BAD_REQUEST]")
        upstreamError.getMessage must include(upstreamBody)
      }

      "must fail with UpstreamErrorResponse when downstream returns SERVICE_UNAVAILABLE" in new TestSetup {

        val upstreamBody = "Service unavailable"

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, upstreamBody)))

        val thrown = connector
          .sendCode("test@example.com")
          .failed
          .futureValue

        thrown mustBe a[UpstreamErrorResponse]

        val upstreamError = thrown.asInstanceOf[UpstreamErrorResponse]

        upstreamError.statusCode mustEqual SERVICE_UNAVAILABLE
        upstreamError.reportAs mustEqual INTERNAL_SERVER_ERROR
        upstreamError.getMessage must include("Email verification call [POST /v2/send-code] failed")
        upstreamError.getMessage must include(s"upstream status [$SERVICE_UNAVAILABLE]")
        upstreamError.getMessage must include(upstreamBody)
      }

      "must propagate Throwable when an unexpected error occurs" in new TestSetup {

        val runtimeException = new RuntimeException("Connection timeout")

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.failed(runtimeException))

        val thrown = connector
          .sendCode("test@example.com")
          .failed
          .futureValue

        thrown shouldBe runtimeException
      }
    }

    "verifyCode" - {

      "must return Unit when downstream returns OK" in new TestSetup {

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        val result = connector
          .verifyCode("test@example.com", "123456")
          .futureValue

        result shouldBe ()
      }

      "must include request body" in new TestSetup {

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        connector
          .verifyCode("test@example.com", "123456")
          .futureValue

        verify(mockRequestBuilder).withBody(any())(any(), any(), any())
      }

      "must fail with UpstreamErrorResponse when downstream returns BAD_REQUEST" in new TestSetup {

        val upstreamBody = """{"code":"CODE_NOT_VALID"}"""

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, upstreamBody)))

        val thrown = connector
          .verifyCode("test@example.com", "123456")
          .failed
          .futureValue

        thrown mustBe a[UpstreamErrorResponse]

        val upstreamError = thrown.asInstanceOf[UpstreamErrorResponse]

        upstreamError.statusCode mustEqual BAD_REQUEST
        upstreamError.reportAs mustEqual INTERNAL_SERVER_ERROR
        upstreamError.getMessage must include("Email verification call [POST /v2/verify-code] failed")
        upstreamError.getMessage must include(s"upstream status [$BAD_REQUEST]")
        upstreamError.getMessage must include(upstreamBody)
      }

      "must fail with UpstreamErrorResponse when downstream returns SERVICE_UNAVAILABLE" in new TestSetup {

        val upstreamBody = "Service unavailable"

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, upstreamBody)))

        val thrown = connector
          .verifyCode("test@example.com", "123456")
          .failed
          .futureValue

        thrown mustBe a[UpstreamErrorResponse]

        val upstreamError = thrown.asInstanceOf[UpstreamErrorResponse]

        upstreamError.statusCode mustEqual SERVICE_UNAVAILABLE
        upstreamError.reportAs mustEqual INTERNAL_SERVER_ERROR
        upstreamError.getMessage must include("Email verification call [POST /v2/verify-code] failed")
        upstreamError.getMessage must include(s"upstream status [$SERVICE_UNAVAILABLE]")
        upstreamError.getMessage must include(upstreamBody)
      }

      "must propagate Throwable when an unexpected error occurs" in new TestSetup {

        val runtimeException = new RuntimeException("Connection timeout")

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.failed(runtimeException))

        val thrown = connector
          .verifyCode("test@example.com", "123456")
          .failed
          .futureValue

        thrown shouldBe runtimeException
      }
    }
  }
}
