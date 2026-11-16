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
import models.emailverification.VerifyEmailCodeResult.{InvalidCode, Verified}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.http.Status.*
import play.api.inject
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.Future

class EmailVerificationConnectorSpec extends SpecBase {

  private val testEmail                 = "test@example.com"
  private val testVerificationCode      = "123456"
  private val serviceUnavailableMessage = "Service unavailable"
  private val connectionTimeoutMessage  = "Connection timeout"
  private val sendCodeFailureMessage    = "Email verification call [POST /v2/send-code] failed"

  trait TestSetup {

    val connector: EmailVerificationConnector = applicationBuilder(
      journeyData = None,
      overrides = Seq(
        inject.bind[HttpClientV2].toInstance(mockHttpClient),
        inject.bind[FrontendAppConfig].toInstance(mockAppConfig)
      )
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
          .sendCode(testEmail)
          .futureValue

        result shouldBe ()
      }

      "must include request body" in new TestSetup {

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        connector
          .sendCode(testEmail)
          .futureValue

        verify(mockRequestBuilder).withBody(any())(any(), any(), any())
      }

      "must fail with UpstreamErrorResponse when downstream returns BAD_REQUEST" in new TestSetup {

        val upstreamBody = """{"code":"INVALID_REQUEST"}"""

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, upstreamBody)))

        val thrown = connector
          .sendCode(testEmail)
          .failed
          .futureValue

        thrown mustBe a[UpstreamErrorResponse]

        val upstreamError = thrown.asInstanceOf[UpstreamErrorResponse]

        upstreamError.statusCode mustEqual BAD_REQUEST
        upstreamError.reportAs mustEqual INTERNAL_SERVER_ERROR
        upstreamError.getMessage must include(sendCodeFailureMessage)
        upstreamError.getMessage must include(s"upstream status [$BAD_REQUEST]")
        upstreamError.getMessage must include(upstreamBody)
        verify(mockRequestBuilder).execute[HttpResponse](any(), any())
      }

      "must fail with UpstreamErrorResponse when downstream returns SERVICE_UNAVAILABLE" in new TestSetup {

        val upstreamBody = serviceUnavailableMessage

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, upstreamBody)))

        val thrown = connector
          .sendCode(testEmail)
          .failed
          .futureValue

        thrown mustBe a[UpstreamErrorResponse]

        val upstreamError = thrown.asInstanceOf[UpstreamErrorResponse]

        upstreamError.statusCode mustEqual SERVICE_UNAVAILABLE
        upstreamError.reportAs mustEqual INTERNAL_SERVER_ERROR
        upstreamError.getMessage must include(sendCodeFailureMessage)
        upstreamError.getMessage must include(s"upstream status [$SERVICE_UNAVAILABLE]")
        upstreamError.getMessage must include(upstreamBody)
        verify(mockRequestBuilder, times(callAmountWithRetries)).execute[HttpResponse](any(), any())
      }

      "must propagate Throwable when an unexpected error occurs" in new TestSetup {

        val runtimeException = new RuntimeException(connectionTimeoutMessage)

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.failed(runtimeException))

        val thrown = connector
          .sendCode(testEmail)
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
          .verifyCode(testEmail, testVerificationCode)
          .futureValue

        result shouldBe Verified
      }

      "must include request body" in new TestSetup {

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        connector
          .verifyCode(testEmail, testVerificationCode)
          .futureValue

        verify(mockRequestBuilder).withBody(any())(any(), any(), any())
      }

      "must return InvalidCode when downstream returns BAD_REQUEST" in new TestSetup {

        val upstreamBody = """{"code":"CODE_NOT_VALID"}"""

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, upstreamBody)))

        val result = connector
          .verifyCode(testEmail, testVerificationCode)
          .futureValue

        result mustBe InvalidCode
        verify(mockRequestBuilder).execute[HttpResponse](any(), any())
      }

      "must fail with UpstreamErrorResponse when downstream returns SERVICE_UNAVAILABLE" in new TestSetup {

        val upstreamBody = serviceUnavailableMessage

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, upstreamBody)))

        val thrown = connector
          .verifyCode(testEmail, testVerificationCode)
          .failed
          .futureValue

        thrown mustBe a[UpstreamErrorResponse]

        val upstreamError = thrown.asInstanceOf[UpstreamErrorResponse]

        upstreamError.statusCode mustEqual SERVICE_UNAVAILABLE
        upstreamError.reportAs mustEqual INTERNAL_SERVER_ERROR
        upstreamError.getMessage must include("Email verification call [POST /v2/verify-code] failed")
        upstreamError.getMessage must include(s"upstream status [$SERVICE_UNAVAILABLE]")
        upstreamError.getMessage must include(upstreamBody)
        verify(mockRequestBuilder, times(callAmountWithRetries)).execute[HttpResponse](any(), any())
      }

      "must propagate Throwable when an unexpected error occurs" in new TestSetup {

        val runtimeException = new RuntimeException(connectionTimeoutMessage)

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.failed(runtimeException))

        val thrown = connector
          .verifyCode(testEmail, testVerificationCode)
          .failed
          .futureValue

        thrown shouldBe runtimeException
      }
    }
  }
}
