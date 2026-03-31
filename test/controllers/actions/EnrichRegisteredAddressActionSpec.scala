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

package controllers.actions

import base.SpecBase
import models.journeydata.JourneyData
import models.requests.OptionalDataRequest
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.test.FakeRequest
import services.RegisteredAddressUprnService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class EnrichRegisteredAddressActionSpec extends SpecBase with MockitoSugar {

  class Harness(service: RegisteredAddressUprnService)
    extends EnrichRegisteredAddressAction(service) {

    def callFilter[A](request: OptionalDataRequest[A]): Future[Option[Result]] =
      filter(request)
  }

  private val mockUprnService = mock[RegisteredAddressUprnService]
  private val action          = new Harness(mockUprnService)

  private val providerId = "providerId"
  private val groupId    = testGroupId

  private def buildRequest(journeyData: Option[JourneyData]): OptionalDataRequest[_] =
    OptionalDataRequest(
      request = FakeRequest(),
      groupId = groupId,
      credentials = testCredentials.copy(providerId = providerId),
      credentialRole = testCredentialRoleUser,
      journeyData = journeyData
    )

  "EnrichRegisteredAddressAction" - {

    "when enrichment succeeds" - {

      "must call enrichment service and allow request to proceed" in {

        val request = buildRequest(Some(mock[JourneyData]))

        when(
          mockUprnService.enrichUprnIfMissing(
            eqTo(groupId),
            eqTo(providerId),
            eqTo(request.journeyData)
          )(any[HeaderCarrier])
        ).thenReturn(Future.successful(()))

        val result = action.callFilter(request).futureValue

        result mustBe None

        verify(mockUprnService).enrichUprnIfMissing(
          eqTo(groupId),
          eqTo(providerId),
          eqTo(request.journeyData)
        )(any[HeaderCarrier])
      }

      "must proceed when no journey data is present" in {

        val request = buildRequest(None)

        when(
          mockUprnService.enrichUprnIfMissing(
            eqTo(groupId),
            eqTo(providerId),
            eqTo(None)
          )(any[HeaderCarrier])
        ).thenReturn(Future.successful(()))

        val result = action.callFilter(request).futureValue

        result mustBe None

        verify(mockUprnService).enrichUprnIfMissing(
          eqTo(groupId),
          eqTo(providerId),
          eqTo(None)
        )(any[HeaderCarrier])
      }
    }

    "when enrichment fails" - {

      "must not block request when service fails" in {

        val request = buildRequest(Some(mock[JourneyData]))

        when(
          mockUprnService.enrichUprnIfMissing(
            any(),
            any(),
            any()
          )(any[HeaderCarrier])
        ).thenReturn(Future.failed(new RuntimeException("boom")))

        val result = action.callFilter(request).futureValue

        result mustBe None

        verify(mockUprnService).enrichUprnIfMissing(
          eqTo(groupId),
          eqTo(providerId),
          eqTo(request.journeyData)
        )(any[HeaderCarrier])
      }
    }

    "must always allow request to proceed regardless of outcome" in {

      val request = buildRequest(Some(mock[JourneyData]))

      when(
        mockUprnService.enrichUprnIfMissing(
          any(),
          any(),
          any()
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(()))

      action.callFilter(request).futureValue mustBe None
    }
  }
}