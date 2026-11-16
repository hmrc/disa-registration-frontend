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
import play.api.http.Status.{OK, SERVICE_UNAVAILABLE}
import play.api.test.Helpers.await
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.BaseIntegrationSpec
import utils.WiremockHelper.stubPost

class AddressLookupConnectorISpec extends BaseIntegrationSpec {

  private val testPostcode              = "BB00 0BB"
  private val testFilter                = "Test"
  private val testCountryName           = "United Kingdom"
  private val serviceUnavailableMessage = "Service unavailable"

  val connector: AddressLookupConnector = app.injector.instanceOf[AddressLookupConnector]

  private val basePath = "/address-lookup/lookup"

  "AddressLookupConnector.searchAddress" should {

    "return parsed Address sequence when backend returns 200 OK" in {
      val responseBody =
        s"""
          |[
          |  {
          |    "id": "GB1",
          |    "uprn": 123456789,
          |    "address": {
          |      "lines": ["1 Test Street", "Test Area"],
          |      "town": "Test Town",
          |      "postcode": "$testPostcode",
          |      "country": {
          |        "code": "GB",
          |        "name": "$testCountryName"
          |      }
          |    }
          |  }
          |]
          |""".stripMargin

      stubPost(basePath, OK, responseBody)

      val result = await(connector.searchAddress(testPostcode, Some(testFilter)))

      result shouldBe Seq(
        LookupAddress(
          addressLine1 = Some("1 Test Street"),
          addressLine2 = Some("Test Area"),
          addressLine3 = Some("Test Town"),
          postCode = Some(testPostcode),
          uprn = Some("123456789"),
          country = Some("United Kingdom")
        )
      )
    }

    "send correct request body to downstream" in {

      val expectedRequestBody =
        s"""
          |{
          |  "postcode": "$testPostcode",
          |  "filter": "$testFilter"
          |}
          |""".stripMargin

      val responseBody =
        s"""
          |[
          |  {
          |    "uprn": 200000698110,
          |    "address": {
          |      "lines": ["Test Street"],
          |      "postcode": "$testPostcode",
          |      "country": {
          |        "code": "GB",
          |        "name": "United Kingdom"
          |      }
          |    }
          |  }
          |]
          |""".stripMargin

      stubPost(basePath, OK, responseBody)

      await(connector.searchAddress(testPostcode, Some(testFilter)))

      verify(
        postRequestedFor(urlEqualTo(basePath))
          .withRequestBody(equalToJson(expectedRequestBody))
      )
    }

    "propagate UpstreamErrorResponse when backend returns an error" in {

      val errorResponse =
        s"""
          |{
          |  "message": "$serviceUnavailableMessage"
          |}
          |""".stripMargin

      stubPost(basePath, SERVICE_UNAVAILABLE, errorResponse)

      val ex = intercept[UpstreamErrorResponse] {
        await(connector.searchAddress(testPostcode, None))
      }

      ex.statusCode shouldBe SERVICE_UNAVAILABLE
      ex.getMessage should include(serviceUnavailableMessage)
      verify(callAmountWithRetries, postRequestedFor(urlEqualTo(basePath)))
    }
  }
}
