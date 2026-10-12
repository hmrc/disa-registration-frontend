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
import services.CorrespondenceAddressUprnService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class EnrichCorrespondenceAddressUprnActionSpec extends SpecBase with MockitoSugar {

  class Harness(service: CorrespondenceAddressUprnService) extends EnrichCorrespondenceAddressUprnAction(service) {
    def callFilter[A](request: OptionalDataRequest[A]): Future[Option[Result]] =
      filter(request)
  }

  private val mockUprnService = mock[CorrespondenceAddressUprnService]
  private val action          = new Harness(mockUprnService)
  private val providerId      = "providerId"
  private val groupId         = testGroupId

  private val testJourneyData: JourneyData =
    JourneyData(groupId = groupId, enrolmentId = "enrolmentId", organisationDetails = Some(testOrganisationDetails))

  private def buildRequest(journeyData: Option[JourneyData]): OptionalDataRequest[_] =
    OptionalDataRequest(
      request = FakeRequest(),
      groupId = groupId,
      credentials = testCredentials.copy(providerId = providerId),
      credentialRole = testCredentialRoleUser,
      journeyData = journeyData
    )

  "EnrichCorrespondenceAddressUprnAction" - {
    "must call enrichment and allow the request to proceed" in {
      val request = buildRequest(Some(testJourneyData))
      when(
        mockUprnService.enrichUprnIfMissing(eqTo(groupId), eqTo(providerId), eqTo(request.journeyData))(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(()))

      action.callFilter(request).futureValue mustBe None

      verify(mockUprnService).enrichUprnIfMissing(eqTo(groupId), eqTo(providerId), eqTo(request.journeyData))(
        any[HeaderCarrier]
      )
    }

    "must proceed when no journey data is present" in {
      val request = buildRequest(None)
      when(
        mockUprnService.enrichUprnIfMissing(eqTo(groupId), eqTo(providerId), eqTo(None))(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(()))

      action.callFilter(request).futureValue mustBe None
    }

    "must remain non-blocking when enrichment fails" in {
      val request = buildRequest(Some(testJourneyData))
      when(mockUprnService.enrichUprnIfMissing(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      action.callFilter(request).futureValue mustBe None
    }
  }
}
