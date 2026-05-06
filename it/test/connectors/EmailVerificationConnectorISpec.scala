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

import com.github.tomakehurst.wiremock.client.WireMock.{equalToJson, postRequestedFor, urlEqualTo, verify}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SERVICE_UNAVAILABLE}
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.BaseIntegrationSpec
import utils.WiremockHelper.stubPost

class EmailVerificationConnectorISpec extends BaseIntegrationSpec {

  val connector: EmailVerificationConnector = app.injector.instanceOf[EmailVerificationConnector]

  private val sendCodePath   = "/email-verification/v2/send-code"
  private val verifyCodePath = "/email-verification/v2/verify-code"

  "EmailVerificationConnector.sendCode" should {

    "return Unit when email-verification returns 200 OK" in {

      val responseBody =
        """
          |{
          |  "result": "sent"
          |}
          |""".stripMargin

      stubPost(sendCodePath, OK, responseBody)

      val result = await(connector.sendCode("test@example.com"))

      result shouldBe ()
    }

    "send the correct request body to email-verification" in {

      val expectedRequestBody =
        Json.parse(
          """
            |{
            |  "email": "test@example.com"
            |}
            |""".stripMargin
        )

      val responseBody =
        """
          |{
          |  "result": "sent"
          |}
          |""".stripMargin

      stubPost(sendCodePath, OK, responseBody)

      await(connector.sendCode("test@example.com"))

      verify(
        postRequestedFor(urlEqualTo(sendCodePath))
          .withRequestBody(equalToJson(expectedRequestBody.toString))
      )
    }

    "throw UpstreamErrorResponse when email-verification returns 400 Bad Request" in {

      val errorResponse =
        """
          |{
          |  "message": "Invalid request"
          |}
          |""".stripMargin

      stubPost(sendCodePath, BAD_REQUEST, errorResponse)

      val ex = intercept[UpstreamErrorResponse] {
        await(connector.sendCode("test@example.com"))
      }

      ex.statusCode shouldBe BAD_REQUEST
      ex.reportAs shouldBe INTERNAL_SERVER_ERROR
      ex.getMessage should include("Email verification call [POST /v2/send-code] failed")
      ex.getMessage should include(s"upstream status [$BAD_REQUEST]")
      ex.getMessage should include("Invalid request")
    }

    "throw UpstreamErrorResponse when email-verification returns 503 Service Unavailable" in {

      val errorResponse =
        """
          |{
          |  "message": "Service unavailable"
          |}
          |""".stripMargin

      stubPost(sendCodePath, SERVICE_UNAVAILABLE, errorResponse)

      val ex = intercept[UpstreamErrorResponse] {
        await(connector.sendCode("test@example.com"))
      }

      ex.statusCode shouldBe SERVICE_UNAVAILABLE
      ex.reportAs shouldBe INTERNAL_SERVER_ERROR
      ex.getMessage should include("Email verification call [POST /v2/send-code] failed")
      ex.getMessage should include(s"upstream status [$SERVICE_UNAVAILABLE]")
      ex.getMessage should include("Service unavailable")
    }
  }

  "EmailVerificationConnector.verifyCode" should {

    "return Unit when email-verification returns 200 OK" in {

      val responseBody =
        """
          |{
          |  "result": "verified"
          |}
          |""".stripMargin

      stubPost(verifyCodePath, OK, responseBody)

      val result = await(connector.verifyCode("test@example.com", "123456"))

      result shouldBe ()
    }

    "send the correct request body to email-verification" in {

      val expectedRequestBody =
        Json.parse(
          """
            |{
            |  "email": "test@example.com",
            |  "code": "123456"
            |}
            |""".stripMargin
        )

      val responseBody =
        """
          |{
          |  "result": "verified"
          |}
          |""".stripMargin

      stubPost(verifyCodePath, OK, responseBody)

      await(connector.verifyCode("test@example.com", "123456"))

      verify(
        postRequestedFor(urlEqualTo(verifyCodePath))
          .withRequestBody(equalToJson(expectedRequestBody.toString))
      )
    }

    "throw UpstreamErrorResponse when email-verification returns 400 Bad Request" in {

      val errorResponse =
        """
          |{
          |  "message": "Code not valid"
          |}
          |""".stripMargin

      stubPost(verifyCodePath, BAD_REQUEST, errorResponse)

      val ex = intercept[UpstreamErrorResponse] {
        await(connector.verifyCode("test@example.com", "123456"))
      }

      ex.statusCode shouldBe BAD_REQUEST
      ex.reportAs shouldBe INTERNAL_SERVER_ERROR
      ex.getMessage should include("Email verification call [POST /v2/verify-code] failed")
      ex.getMessage should include(s"upstream status [$BAD_REQUEST]")
      ex.getMessage should include("Code not valid")
    }

    "throw UpstreamErrorResponse when email-verification returns 503 Service Unavailable" in {

      val errorResponse =
        """
          |{
          |  "message": "Service unavailable"
          |}
          |""".stripMargin

      stubPost(verifyCodePath, SERVICE_UNAVAILABLE, errorResponse)

      val ex = intercept[UpstreamErrorResponse] {
        await(connector.verifyCode("test@example.com", "123456"))
      }

      ex.statusCode shouldBe SERVICE_UNAVAILABLE
      ex.reportAs shouldBe INTERNAL_SERVER_ERROR
      ex.getMessage should include("Email verification call [POST /v2/verify-code] failed")
      ex.getMessage should include(s"upstream status [$SERVICE_UNAVAILABLE]")
      ex.getMessage should include("Service unavailable")
    }
  }
}