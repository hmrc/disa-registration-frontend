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
import models.addresslookup.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.http.Status.SERVICE_UNAVAILABLE
import play.api.inject
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.Future

class AddressLookupConnectorSpec extends SpecBase {

  private val testPostcode     = "BB00 0BB"
  private val testFilter       = "Test"
  private val testCountryCode  = "GB"
  private val testCountryName  = "United Kingdom"
  private val testAddressLine1 = "1 Test Street"
  private val testAddressLine2 = "Test Area"
  private val testTown         = "Testtown"

  trait TestSetup {

    val connector: AddressLookupConnector =
      applicationBuilder(
        journeyData = None,
        overrides = Seq(
          inject.bind[HttpClientV2].toInstance(mockHttpClient),
          inject.bind[FrontendAppConfig].toInstance(mockAppConfig)
        )
      ).build().injector.instanceOf[AddressLookupConnector]

    val testUrl: String =
      "http://localhost:9999"

    when(mockAppConfig.addressLookupBaseUrl)
      .thenReturn(testUrl)

    when(mockHttpClient.post(any())(any()))
      .thenReturn(mockRequestBuilder)

    when(mockRequestBuilder.withBody(any())(any(), any(), any()))
      .thenReturn(mockRequestBuilder)
  }

  "AddressLookupConnector" - {

    "searchAddress" - {

      "must return parsed Address sequence on success" in new TestSetup {

        val response =
          Seq(
            AddressLookupResponse(
              address = AddressLookupAddress(
                lines = Seq(testAddressLine1, testAddressLine2),
                town = Some(testTown),
                postcode = Some(testPostcode),
                country = Some(
                  AddressLookupCountry(
                    code = testCountryCode,
                    name = testCountryName
                  )
                )
              ),
              uprn = Some(123)
            )
          )

        val expected =
          Seq(
            LookupAddress(
              addressLine1 = Some(testAddressLine1),
              addressLine2 = Some(testAddressLine2),
              addressLine3 = Some(testTown),
              postCode = Some(testPostcode),
              uprn = Some("123"),
              country = Some(testCountryName)
            )
          )

        when(
          mockRequestBuilder.execute[Seq[AddressLookupResponse]](any(), any())
        ).thenReturn(Future.successful(response))

        val result =
          connector.searchAddress(testPostcode, Some(testFilter)).futureValue

        result shouldBe expected
      }

      "must include request body with postcode and filter" in new TestSetup {

        val response =
          Seq(
            AddressLookupResponse(
              address = AddressLookupAddress(
                lines = Seq("Test Street"),
                town = None,
                postcode = Some(testPostcode),
                country = Some(
                  AddressLookupCountry(
                    code = testCountryCode,
                    name = testCountryName
                  )
                )
              ),
              uprn = Some(200)
            )
          )

        when(
          mockRequestBuilder.execute[Seq[AddressLookupResponse]](any(), any())
        ).thenReturn(Future.successful(response))

        connector.searchAddress(testPostcode, Some(testFilter)).futureValue

        verify(mockRequestBuilder)
          .withBody(any())(any, any, any)
      }

      "must propagate UpstreamErrorResponse when downstream fails" in new TestSetup {

        val upstreamErrorResponse =
          UpstreamErrorResponse(
            "Service unavailable",
            SERVICE_UNAVAILABLE,
            SERVICE_UNAVAILABLE,
            Map.empty
          )

        when(
          mockRequestBuilder.execute[Seq[AddressLookupResponse]](any(), any())
        ).thenReturn(Future.failed(upstreamErrorResponse))

        val thrown =
          connector.searchAddress(testPostcode, None).failed.futureValue

        thrown shouldBe upstreamErrorResponse
        verify(mockRequestBuilder, times(callAmountWithRetries))
          .execute[Seq[AddressLookupResponse]](any(), any())
      }

      "must retry when the request fails with a BadGatewayException" in new TestSetup {

        val exception = new BadGatewayException("Bad gateway")

        when(
          mockRequestBuilder.execute[Seq[AddressLookupResponse]](any(), any())
        ).thenReturn(Future.failed(exception))

        connector.searchAddress(testPostcode, None).failed.futureValue shouldBe exception
        verify(mockRequestBuilder, times(callAmountWithRetries))
          .execute[Seq[AddressLookupResponse]](any(), any())
      }

      "must retry when the request fails with a GatewayTimeoutException" in new TestSetup {

        val exception = new GatewayTimeoutException("Gateway timeout")

        when(
          mockRequestBuilder.execute[Seq[AddressLookupResponse]](any(), any())
        ).thenReturn(Future.failed(exception))

        connector.searchAddress(testPostcode, None).failed.futureValue shouldBe exception
        verify(mockRequestBuilder, times(callAmountWithRetries))
          .execute[Seq[AddressLookupResponse]](any(), any())
      }

      "must propagate Throwable when unexpected error occurs" in new TestSetup {

        val exception =
          new RuntimeException("Connection timeout")

        when(
          mockRequestBuilder.execute[Seq[AddressLookupResponse]](any(), any())
        ).thenReturn(Future.failed(exception))

        val thrown =
          connector.searchAddress(testPostcode, None).failed.futureValue

        thrown shouldBe exception
      }
    }
  }
}
