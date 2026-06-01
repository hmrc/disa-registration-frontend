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
import models.taxenrolments.TaxEnrolmentSubscription
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.inject
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{StringContextOps, UpstreamErrorResponse}

import scala.concurrent.Future

class TaxEnrolmentsConnectorSpec extends SpecBase {

  trait TestSetup {
    val connector: TaxEnrolmentsConnector = applicationBuilder(
      journeyData = None,
      overrides = Seq(
        inject.bind[HttpClientV2].toInstance(mockHttpClient),
        inject.bind[FrontendAppConfig].toInstance(mockAppConfig)
      )
    ).build().injector.instanceOf[TaxEnrolmentsConnector]

    val testUrl: String = "http://localhost:1203"

    when(mockAppConfig.taxEnrolmentsBaseUrl).thenReturn(testUrl)
    when(mockHttpClient.get(url"$testUrl/tax-enrolments/groups/$testGroupId/subscriptions"))
      .thenReturn(mockRequestBuilder)
  }

  "TaxEnrolmentsConnector" - {

    "getSubscriptionsByGroupId" - {

      "must return subscriptions on a successful response" in new TestSetup {
        val subscriptions = Seq(
          TaxEnrolmentSubscription(
            serviceName = "HMRC-DISA-ORG",
            state = "PENDING",
            groupIdentifier = Some(testGroupId)
          )
        )

        when(mockRequestBuilder.execute[Seq[TaxEnrolmentSubscription]](any(), any()))
          .thenReturn(Future.successful(subscriptions))

        connector.getSubscriptionsByGroupId(testGroupId).futureValue mustBe subscriptions
      }

      "must propagate upstream errors" in new TestSetup {
        val upstreamErrorResponse = UpstreamErrorResponse(
          message = "Bad request",
          statusCode = 400,
          reportAs = 400,
          headers = Map.empty
        )

        when(mockRequestBuilder.execute[Seq[TaxEnrolmentSubscription]](any(), any()))
          .thenReturn(Future.failed(upstreamErrorResponse))

        connector.getSubscriptionsByGroupId(testGroupId).failed.futureValue mustBe upstreamErrorResponse
      }
    }
  }
}
