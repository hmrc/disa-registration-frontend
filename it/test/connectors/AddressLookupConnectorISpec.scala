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
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.BaseIntegrationSpec
import utils.WiremockHelper.stubPost

class AddressLookupConnectorISpec extends BaseIntegrationSpec {

  val connector: AddressLookupConnector = app.injector.instanceOf[AddressLookupConnector]

  private val basePath = "/lookup"

  "AddressLookupConnector.searchAddress" should {

    "return JSON response when backend returns 200 OK" in {

      val responseBody =
        """
          |[
          |  {
          |    "id": "GB1",
          |    "uprn": 123456789
          |  }
          |]
          |""".stripMargin

      stubPost(basePath, 200, responseBody)

      val result = await(connector.searchAddress("BB00 0BB", Some("Test")))

      result shouldBe Json.parse(responseBody)
    }

    "send correct request body to downstream" in {

      val expectedRequestBody =
        Json.parse(
          """
            |{
            |  "postcode": "BB00 0BB",
            |  "filter": "Test"
            |}
            |""".stripMargin
        )

      val responseBody =
        """
          |[
          |  {
          |    "id": "GB200000698110",
          |    "uprn": 200000698110
          |  }
          |]
          |""".stripMargin

      stubPost(basePath, 200, responseBody)

      await(connector.searchAddress("BB00 0BB", Some("Test")))

      verify(
        postRequestedFor(urlEqualTo(basePath))
          .withRequestBody(equalToJson(expectedRequestBody.toString))
      )
    }

    "propagate UpstreamErrorResponse when backend returns an error" in {

      val errorResponse =
        """
          |{
          |  "message": "Service unavailable"
          |}
          |""".stripMargin

      stubPost(basePath, 503, errorResponse)

      val ex = intercept[UpstreamErrorResponse] {
        await(connector.searchAddress("BB00 0BB", None))
      }

      ex.statusCode shouldBe 503
      ex.getMessage should include("Service unavailable")
    }
  }
}