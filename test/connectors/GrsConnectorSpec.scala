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
import models.grs.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api.http.Status.SERVICE_UNAVAILABLE
import play.api.inject
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.Future

class GrsConnectorSpec extends SpecBase {

  private val serviceUnavailableMessage = "Service unavailable"

  trait TestSetup {
    val connector: GrsConnector = applicationBuilder(
      journeyData = None,
      overrides = Seq(
        inject.bind[HttpClientV2].toInstance(mockHttpClient),
        inject.bind[FrontendAppConfig].toInstance(mockAppConfig)
      )
    ).build().injector.instanceOf[GrsConnector]

    val testUrl: String = "http://localhost:1203"

    when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
    when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.setHeader("Content-Type" -> "application/json"))
      .thenReturn(mockRequestBuilder)
  }

  "GrsConnector" - {

    "createJourney" - {
      "must retry upstream 5xx errors" in new TestSetup {
        val request = GrsCreateJourneyRequest(
          continueUrl = "/continue",
          businessVerificationCheck = true,
          deskProServiceId = "service",
          signOutUrl = "/sign-out",
          regime = "DISA",
          accessibilityUrl = "/accessibility",
          labels = None
        )
        val upstreamErrorResponse = UpstreamErrorResponse(serviceUnavailableMessage, SERVICE_UNAVAILABLE)

        when(mockAppConfig.grsCreateJourneyUrl(GrsCompanyType.LimitedCompany)).thenReturn(testUrl)
        when(mockRequestBuilder.execute[CreateJourneyResponse](any(), any()))
          .thenReturn(Future.failed(upstreamErrorResponse))

        connector.createJourney(GrsCompanyType.LimitedCompany, request).failed.futureValue mustBe upstreamErrorResponse
        verify(mockRequestBuilder, times(callAmountWithRetries))
          .execute[CreateJourneyResponse](any(), any())
      }
    }

    "fetchJourneyData" - {
      "must retry upstream 5xx errors" in new TestSetup {
        val journeyId = "testJourneyId"
        val upstreamErrorResponse = UpstreamErrorResponse(serviceUnavailableMessage, SERVICE_UNAVAILABLE)

        when(mockAppConfig.grsRetrieveResultUrl(GrsCompanyType.LimitedCompany, journeyId)).thenReturn(testUrl)
        when(mockRequestBuilder.execute[IncorporatedEntityGRSResponse](any(), any()))
          .thenReturn(Future.failed(upstreamErrorResponse))

        connector.fetchJourneyData(GrsCompanyType.LimitedCompany, journeyId).failed.futureValue mustBe
          upstreamErrorResponse
        verify(mockRequestBuilder, times(callAmountWithRetries))
          .execute[IncorporatedEntityGRSResponse](any(), any())
      }
    }
  }
}
