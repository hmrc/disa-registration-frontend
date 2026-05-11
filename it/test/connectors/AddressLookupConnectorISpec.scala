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

import com.github.tomakehurst.wiremock.client.WireMock.*
import models.addresslookup.LookupAddress
import play.api.test.Helpers.await
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.BaseIntegrationSpec
import utils.WiremockHelper.stubPost

class AddressLookupConnectorISpec extends BaseIntegrationSpec {

  val connector: AddressLookupConnector = app.injector.instanceOf[AddressLookupConnector]

  private val basePath = "/address-lookup/lookup"

  "AddressLookupConnector.searchAddress" should {

    "return parsed LookupAddress sequence when backend returns 200 OK" in {

      val responseBody =
        """
          |[
          |  {
          |    "id": "GB1",
          |    "uprn": 123456789,
          |    "address": {
          |      "lines": ["1 Test Street", "Test Area"],
          |      "town": "Test Town",
          |      "postcode": "BB00 0BB"
          |    }
          |  }
          |]
          |""".stripMargin

      stubPost(basePath, 200, responseBody)

      val result = await(connector.searchAddress("BB00 0BB", Some("Test")))

      result shouldBe Seq(
        LookupAddress(
          addressLine1 = Some("1 Test Street"),
          addressLine2 = Some("Test Area"),
          addressLine3 = Some("Test Town"),
          postCode = Some("BB00 0BB"),
          uprn = Some("123456789")
        )
      )
    }

    "send correct request body to downstream" in {

      val expectedRequestBody =
        """
          |{
          |  "postcode": "BB00 0BB",
          |  "filter": "Test"
          |}
          |""".stripMargin

      val responseBody =
        """
          |[
          |  {
          |    "uprn": 200000698110,
          |    "address": {
          |      "lines": ["Test Street"],
          |      "postcode": "BB00 0BB"
          |    }
          |  }
          |]
          |""".stripMargin

      stubPost(basePath, 200, responseBody)

      await(connector.searchAddress("BB00 0BB", Some("Test")))

      verify(
        postRequestedFor(urlEqualTo(basePath))
          .withRequestBody(equalToJson(expectedRequestBody))
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