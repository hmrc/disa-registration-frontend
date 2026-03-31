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
import play.api.inject
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.Future

class AddressLookupConnectorSpec extends SpecBase {

  trait TestSetup {

    val connector: AddressLookupConnector = applicationBuilder(
      None,
      inject.bind[HttpClientV2].toInstance(mockHttpClient),
      inject.bind[FrontendAppConfig].toInstance(mockAppConfig)
    ).build().injector.instanceOf[AddressLookupConnector]

    val testUrl: String = "http://localhost:9999"

    when(mockAppConfig.addressLookupBaseUrl).thenReturn(testUrl)

    when(mockHttpClient.post(any())(any()))
      .thenReturn(mockRequestBuilder)

    when(mockRequestBuilder.withBody(any())(any(), any(), any()))
      .thenReturn(mockRequestBuilder)
  }

  "AddressLookupConnector" - {

    "searchAddress" - {

      "must return JsValue on successful call" in new TestSetup {

        val jsonResponse: JsValue = Json.parse(
          """
            |[
            |  {
            |    "id": "GB690091234501",
            |    "uprn": 690091234501,
            |    "address": {
            |      "lines": ["1 Test Street"],
            |      "town": "Testtown",
            |      "postcode": "AA00 0AA"
            |    }
            |  }
            |]
            |""".stripMargin
        )

        when(mockRequestBuilder.execute[JsValue](any(), any()))
          .thenReturn(Future.successful(jsonResponse))

        val result = connector
          .searchAddress("BB00 0BB", Some("Test"))
          .futureValue

        result shouldBe jsonResponse
      }

      "must include request body with postcode and filter" in new TestSetup {

        val response: JsValue = Json.parse(
          """
            |[
            |  {
            |    "id": "GB200000698110",
            |    "uprn": 200000698110,
            |    "address": {
            |      "lines": ["Test Street"],
            |      "postcode": "BB00 0BB"
            |    }
            |  }
            |]
            |""".stripMargin
        )

        when(mockRequestBuilder.execute[JsValue](any(), any()))
          .thenReturn(Future.successful(response))

        connector
          .searchAddress("BB00 0BB", Some("Test"))
          .futureValue

        verify(mockRequestBuilder).withBody(any())(any, any, any)
      }

      "must propagate UpstreamErrorResponse when downstream fails" in new TestSetup {

        val upstreamErrorResponse: UpstreamErrorResponse =
          UpstreamErrorResponse(
            message = "Service unavailable",
            statusCode = 503,
            reportAs = 503,
            headers = Map.empty
          )

        when(mockRequestBuilder.execute[JsValue](any(), any()))
          .thenReturn(Future.failed(upstreamErrorResponse))

        val thrown = connector
          .searchAddress("BB00 0BB", None)
          .failed
          .futureValue

        thrown shouldBe upstreamErrorResponse
      }

      "must propagate Throwable when an unexpected error occurs" in new TestSetup {

        val runtimeException = new RuntimeException("Connection timeout")

        when(mockRequestBuilder.execute[JsValue](any(), any()))
          .thenReturn(Future.failed(runtimeException))

        val thrown = connector
          .searchAddress("BB00 0BB", None)
          .failed
          .futureValue

        thrown shouldBe runtimeException
      }
    }
  }
}
