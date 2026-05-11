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
import config.Constants.defaultUprn
import models.journeydata.RegisteredAddress
import models.addresslookup.LookupAddress
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import play.api.test.Helpers.await
import play.api.test.Helpers.defaultAwaitTimeout

import scala.concurrent.Future

class AddressLookupServiceSpec extends SpecBase {

  private val service = new AddressLookupService(mockAddressLookupConnector)

  private val address = RegisteredAddress(
    addressLine1 = Some("line1"),
    addressLine2 = Some("line2"),
    addressLine3 = Some("line3"),
    postCode = Some("AA1 1AA"),
    uprn = None
  )

  "getUprn" - {

    "must return UPRN when present in lookup results" in {

      val results = Seq(
        LookupAddress(
          addressLine1 = Some("1 Test Street"),
          addressLine2 = None,
          addressLine3 = Some("Test Town"),
          postCode = Some("AA1 1AA"),
          uprn = Some("123456789012")
        )
      )

      when(mockAddressLookupConnector.searchAddress(any[String], any())(any()))
        .thenReturn(Future.successful(results))

      val result = service.getUprn(address).futureValue

      result shouldBe Some("123456789012")
    }

    "must return default UPRN when no UPRN is found in results" in {

      val results = Seq(
        LookupAddress(
          addressLine1 = Some("1 Test Street"),
          addressLine2 = None,
          addressLine3 = Some("Test Town"),
          postCode = Some("AA1 1AA"),
          uprn = None
        )
      )

      when(mockAddressLookupConnector.searchAddress(any[String], any())(any()))
        .thenReturn(Future.successful(results))

      val result = service.getUprn(address).futureValue

      result shouldBe Some(defaultUprn)
    }

    "must return none for UPRN when connector fails" in {

      when(mockAddressLookupConnector.searchAddress(any[String], any())(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val result = service.getUprn(address).futureValue

      result shouldBe None
    }

    "must throw IllegalArgumentException when postcode is missing" in {

      val invalid = address.copy(postCode = None)

      val thrown = intercept[IllegalArgumentException] {
        await(service.getUprn(invalid))
      }

      thrown.getMessage should include("Postcode is required")
    }
  }

  "lookup" - {

    "must return parsed lookup results from connector" in {

      val results = Seq(
        LookupAddress(
          addressLine1 = Some("1 Test Street"),
          addressLine2 = None,
          addressLine3 = Some("Test Town"),
          postCode = Some("AA1 1AA"),
          uprn = Some("111")
        )
      )

      when(mockAddressLookupConnector.searchAddress(any[String], any())(any()))
        .thenReturn(Future.successful(results))

      val result = service.lookup("AA1 1AA", Some("filter")).futureValue

      result shouldBe results
    }

    "must propagate failure when connector fails" in {

      when(mockAddressLookupConnector.searchAddress(any[String], any())(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val thrown = intercept[RuntimeException] {
        await(service.lookup("AA1 1AA", Some("filter")))
      }
      thrown.getMessage shouldBe "boom"
    }
  }
}
