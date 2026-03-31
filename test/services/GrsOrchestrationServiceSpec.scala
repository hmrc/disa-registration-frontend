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
import models.grs.*
import models.journeydata.{BusinessVerification, RegisteredAddress}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.libs.json.Writes
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class GrsOrchestrationServiceSpec extends SpecBase {

  private val service = new GrsOrchestrationService(
    mockGrsService,
    mockAddressLookupService,
    mockJourneyAnswersService
  )

  private val journeyId  = "journeyId"
  private val groupId    = "groupId"
  private val providerId = "providerId"

  private val address = RegisteredAddress(
    addressLine1 = Some("line1"),
    addressLine2 = Some("line2"),
    addressLine3 = Some("line3"),
    postCode = Some("AA1 1AA")
  )

  private val baseGrsResponse = GRSResponse(
    companyNumber = "123",
    companyName = Some("Test Ltd"),
    ctutr = Some("1234567890"),
    chrn = None,
    dateOfIncorporation = None,
    countryOfIncorporation = "GB",
    identifiersMatch = true,
    businessRegistrationStatus = RegisteredStatus,
    businessVerificationStatus = Some(BvPass),
    bpSafeId = None,
    registeredAddress = Some(address)
  )

  "GrsOrchestrationService" - {

    "processGrsJourney" - {

      "must call address lookup and persist enriched address when BV/GRS passed" in {

        val expectedBusinessVerification = BusinessVerification(
          businessVerificationPassed = Some(true),
          businessRegistrationPassed = Some(true),
          ctUtr = Some("1234567890"),
          registeredAddress = Some(address.copy(uprn = Some("999999999999")))
        )

        when(mockGrsService.fetchGRSJourneyData(eqTo(journeyId))(any()))
          .thenReturn(Future.successful(baseGrsResponse))

        when(mockAddressLookupService.getUprn(eqTo(address))(any()))
          .thenReturn(Future.successful("999999999999"))

        when(
          mockJourneyAnswersService.update(
            eqTo(expectedBusinessVerification),
            any[String],
            any[String]
          )(any[Writes[BusinessVerification]], any[HeaderCarrier])
        ).thenReturn(Future.successful(expectedBusinessVerification))

        val result =
          service.processGrsJourney(journeyId, None, groupId, providerId).futureValue

        result shouldBe expectedBusinessVerification

        verify(mockAddressLookupService).getUprn(address)

        verify(mockJourneyAnswersService).update(
          eqTo(expectedBusinessVerification),
          eqTo(groupId),
          eqTo(providerId)
        )(any[Writes[BusinessVerification]], any[HeaderCarrier])
      }

      "must persist failed BV/GRS without calling address lookup" in {

        val failedGrs = baseGrsResponse.copy(
          businessVerificationStatus = Some(BvFail),
          businessRegistrationStatus = FailedStatus
        )

        val expectedBusinessVerification = BusinessVerification(
          businessVerificationPassed = Some(false),
          businessRegistrationPassed = Some(false),
          ctUtr = Some("1234567890"),
          registeredAddress = None
        )

        when(mockGrsService.fetchGRSJourneyData(eqTo(journeyId))(any()))
          .thenReturn(Future.successful(failedGrs))

        when(
          mockJourneyAnswersService.update(
            eqTo(expectedBusinessVerification),
            any[String],
            any[String]
          )(any[Writes[BusinessVerification]], any[HeaderCarrier])
        ).thenReturn(Future.successful(expectedBusinessVerification))

        val result = service.processGrsJourney(journeyId, None, groupId, providerId).futureValue

        result shouldBe expectedBusinessVerification

        verify(mockAddressLookupService, never()).getUprn(any())(any())
      }

      "must fail when BV/GRS passed but no address returned" in {

        val noAddressGrs = baseGrsResponse.copy(registeredAddress = None)

        when(mockGrsService.fetchGRSJourneyData(eqTo(journeyId))(any()))
          .thenReturn(Future.successful(noAddressGrs))

        val thrown =
          service.processGrsJourney(journeyId, None, groupId, providerId).failed.futureValue

        thrown.getMessage shouldBe "Missing address after successful GRS/BV"

        verify(mockJourneyAnswersService, never()).update(any(), any(), any())(any(), any())
      }

      "must fail when address lookup fails" in {

        when(mockGrsService.fetchGRSJourneyData(eqTo(journeyId))(any()))
          .thenReturn(Future.successful(baseGrsResponse))

        when(mockAddressLookupService.getUprn(eqTo(address))(any()))
          .thenReturn(Future.failed(new RuntimeException("Address lookup failed for postcode AA1 1AA")))

        val thrown =
          service.processGrsJourney(journeyId, None, groupId, providerId).failed.futureValue

        thrown.getMessage shouldBe "Address lookup failed for postcode AA1 1AA"

        verify(mockJourneyAnswersService, never()).update(any(), any(), any())(any(), any())
      }

      "must use default uprn when lookup returns default value" in {

        val expectedBusinessVerification = BusinessVerification(
          businessVerificationPassed = Some(true),
          businessRegistrationPassed = Some(true),
          ctUtr = Some("1234567890"),
          registeredAddress = Some(address.copy(uprn = Some("100000000000")))
        )

        when(mockGrsService.fetchGRSJourneyData(eqTo(journeyId))(any()))
          .thenReturn(Future.successful(baseGrsResponse))

        when(mockAddressLookupService.getUprn(eqTo(address))(any()))
          .thenReturn(Future.successful("100000000000"))

        when(
          mockJourneyAnswersService.update(
            eqTo(expectedBusinessVerification),
            any[String],
            any[String]
          )(any[Writes[BusinessVerification]], any[HeaderCarrier])
        ).thenReturn(Future.successful(expectedBusinessVerification))

        val result =
          service.processGrsJourney(journeyId, None, groupId, providerId).futureValue

        result shouldBe expectedBusinessVerification
      }
    }
  }
}
