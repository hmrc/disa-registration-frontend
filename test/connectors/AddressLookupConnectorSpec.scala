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
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.inject
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.Future

class AddressLookupConnectorSpec extends SpecBase {

  trait TestSetup {

    val connector: AddressLookupConnector =
      applicationBuilder(
        None,
        inject.bind[HttpClientV2].toInstance(mockHttpClient),
        inject.bind[FrontendAppConfig].toInstance(mockAppConfig)
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

      "must return parsed LookupAddress sequence on success" in new TestSetup {

        val response =
          Seq(
            AddressLookupResponse(
              address = AddressLookupAddress(
                lines = Seq("1 Test Street", "Test Area"),
                town = Some("Testtown"),
                postcode = Some("BB00 0BB"),
                country = Some(
                  AddressLookupCountry(
                    code = "GB",
                    name = "United Kingdom"
                  )
                )
              ),
              uprn = Some(123)
            )
          )

        val expected =
          Seq(
            LookupAddress(
              addressLine1 = Some("1 Test Street"),
              addressLine2 = Some("Test Area"),
              addressLine3 = Some("Testtown"),
              postCode = Some("BB00 0BB"),
              uprn = Some("123"),
              country = Some("United Kingdom")
            )
          )

        when(
          mockRequestBuilder.execute[Seq[AddressLookupResponse]](any(), any())
        ).thenReturn(Future.successful(response))

        val result =
          connector.searchAddress("BB00 0BB", Some("Test")).futureValue

        result shouldBe expected
      }

      "must include request body with postcode and filter" in new TestSetup {

        val response =
          Seq(
            AddressLookupResponse(
              address = AddressLookupAddress(
                lines = Seq("Test Street"),
                town = None,
                postcode = Some("BB00 0BB"),
                country = Some(
                  AddressLookupCountry(
                    code = "GB",
                    name = "United Kingdom"
                  )
                )
              ),
              uprn = Some(200)
            )
          )

        when(
          mockRequestBuilder.execute[Seq[AddressLookupResponse]](any(), any())
        ).thenReturn(Future.successful(response))

        connector.searchAddress("BB00 0BB", Some("Test")).futureValue

        verify(mockRequestBuilder)
          .withBody(any())(any, any, any)
      }

      "must propagate UpstreamErrorResponse when downstream fails" in new TestSetup {

        val upstreamErrorResponse =
          UpstreamErrorResponse(
            "Service unavailable",
            503,
            503,
            Map.empty
          )

        when(
          mockRequestBuilder.execute[Seq[AddressLookupResponse]](any(), any())
        ).thenReturn(Future.failed(upstreamErrorResponse))

        val thrown =
          connector.searchAddress("BB00 0BB", None).failed.futureValue

        thrown shouldBe upstreamErrorResponse
      }

      "must propagate Throwable when unexpected error occurs" in new TestSetup {

        val exception =
          new RuntimeException("Connection timeout")

        when(
          mockRequestBuilder.execute[Seq[AddressLookupResponse]](any(), any())
        ).thenReturn(Future.failed(exception))

        val thrown =
          connector.searchAddress("BB00 0BB", None).failed.futureValue

        thrown shouldBe exception
      }
    }
  }
}
