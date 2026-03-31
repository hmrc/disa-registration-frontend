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

package services

import base.SpecBase
import models.journeydata.RegisteredAddress
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import play.api.libs.json.Json
import play.api.test.Helpers.await
import play.api.test.Helpers.defaultAwaitTimeout

import scala.concurrent.Future

class AddressLookupServiceSpec extends SpecBase {

  private val service = new AddressLookupService(mockAddressLookupConnector)

  private val address = RegisteredAddress(
    addressLine1 = Some("line1"),
    addressLine2 = Some("line2"),
    addressLine3 = Some("line3"),
    postCode = Some("AA1 1AA")
  )

  "AddressLookupService" - {

    "getUprn" - {

      "must return UPRN from first address in response" in {

        val jsonResponse = Json.parse(
          """
            |[
            |  {
            |    "id": "GB123",
            |    "uprn": 123456789012,
            |    "address": {
            |      "lines": ["line1", "line2"],
            |      "postcode": "AA1 1AA"
            |    }
            |  }
            |]
            |""".stripMargin
        )

        when(mockAddressLookupConnector.searchAddress(any[String], any())(any()))
          .thenReturn(Future.successful(jsonResponse))

        val result = service.getUprn(address).futureValue

        result shouldBe Some("123456789012")

        verify(mockAddressLookupConnector)
          .searchAddress("AA1 1AA", Some("line1"))
      }

      "must return default UPRN when uprn is missing" in {

        val jsonResponse = Json.parse(
          """
            |[
            |  {
            |    "address": {
            |      "lines": ["10 Test Street"],
            |      "postcode": "AA1 1AA"
            |    }
            |  }
            |]
            |""".stripMargin
        )

        when(mockAddressLookupConnector.searchAddress(any[String], any())(any()))
          .thenReturn(Future.successful(jsonResponse))

        val result = service.getUprn(address).futureValue

        result shouldBe Some("100000000000")
      }

      "must return default UPRN when response list is empty" in {

        val jsonResponse = Json.parse(
          """
            |[]
            |""".stripMargin
        )

        when(mockAddressLookupConnector.searchAddress(any[String], any())(any()))
          .thenReturn(Future.successful(jsonResponse))

        val result = service.getUprn(address).futureValue

        result shouldBe Some("100000000000")
      }

      "must return default UPRN when connector fails" in {

        val exception = new RuntimeException("Connector failed")

        when(mockAddressLookupConnector.searchAddress(any[String], any())(any()))
          .thenReturn(Future.failed(exception))

        val result = service.getUprn(address).futureValue

        result shouldBe None
      }

      "must throw IllegalArgumentException when postcode is missing" in {

        val invalidAddress = address.copy(postCode = None)

        val thrown = intercept[IllegalArgumentException] {
          await(service.getUprn(invalidAddress))
        }

        thrown.getMessage should include("Postcode is required")
      }

      "must return UPRN from first address when multiple are returned" in {

        val jsonResponse = Json.parse(
          """
            |[
            |  {
            |    "id": "GB1",
            |    "uprn": 999999999991,
            |    "address": {
            |      "lines": ["First Street"],
            |      "postcode": "AA1 1AA"
            |    }
            |  },
            |  {
            |    "id": "GB2",
            |    "uprn": 999999999999,
            |    "address": {
            |      "lines": ["Second Street"],
            |      "postcode": "AA1 1AA"
            |    }
            |  },
            |  {
            |    "id": "GB3",
            |    "uprn": 123456789012,
            |    "address": {
            |      "lines": ["Third Street"],
            |      "postcode": "AA1 1AA"
            |    }
            |  }
            |]
            |""".stripMargin
        )

        when(mockAddressLookupConnector.searchAddress(any[String], any())(any()))
          .thenReturn(Future.successful(jsonResponse))

        val result = service.getUprn(address).futureValue

        result shouldBe Some("999999999991")
      }
    }
  }
}
