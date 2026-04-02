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
import models.journeydata.{BusinessVerification, JourneyData, RegisteredAddress}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.libs.json.Writes
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class RegisteredAddressUprnServiceSpec extends SpecBase {

  private val service = new RegisteredAddressUprnService(
    mockAddressLookupService,
    mockJourneyAnswersService
  )

  private val testProviderId = "providerId"

  private val baseAddress = RegisteredAddress(
    addressLine1 = Some("line1"),
    addressLine2 = Some("line2"),
    addressLine3 = Some("line3"),
    postCode = Some("AA1 1AA"),
    uprn = None
  )

  private def journeyDataWith(bv: Option[BusinessVerification]) =
    JourneyData(
      groupId = testGroupId,
      enrolmentId = testEnrolmentId,
      businessVerification = bv
    )

  private def bvWith(address: Option[RegisteredAddress]) =
    BusinessVerification(
      businessVerificationPassed = Some(true),
      businessRegistrationPassed = Some(true),
      ctUtr = Some("123"),
      registeredAddress = address,
      companyName = Some("Company name")
    )

  "RegisteredAddressUprnService" - {

    "enrichUprnIfMissing" - {

      "must do nothing when no journey data" in {

        service.enrichUprnIfMissing(testGroupId, testProviderId, None).futureValue shouldBe ((): Unit)

        verify(mockAddressLookupService, never()).getUprn(any())(any())
        verify(mockJourneyAnswersService, never()).update(any(), any(), any())(any(), any())
      }

      "must do nothing when no business verification" in {

        val journeyData = journeyDataWith(None)

        service.enrichUprnIfMissing(testGroupId, testProviderId, Some(journeyData)).futureValue

        verify(mockAddressLookupService, never()).getUprn(any())(any())
        verify(mockJourneyAnswersService, never()).update(any(), any(), any())(any(), any())
      }

      "must do nothing when no registered address" in {

        val journeyData = journeyDataWith(Some(bvWith(None)))

        service.enrichUprnIfMissing(testGroupId, testProviderId, Some(journeyData)).futureValue

        verify(mockAddressLookupService, never()).getUprn(any())(any())
        verify(mockJourneyAnswersService, never()).update(any(), any(), any())(any(), any())
      }

      "must do nothing when uprn already present" in {

        val addressWithUprn = baseAddress.copy(uprn = Some("999999999999"))
        val journeyData     = journeyDataWith(Some(bvWith(Some(addressWithUprn))))

        service.enrichUprnIfMissing(testGroupId, testProviderId, Some(journeyData)).futureValue

        verify(mockAddressLookupService, never()).getUprn(any())(any())
        verify(mockJourneyAnswersService, never()).update(any(), any(), any())(any(), any())
      }

      "must lookup and persist when uprn is missing and lookup succeeds" in {

        val bv          = bvWith(Some(baseAddress))
        val journeyData = journeyDataWith(Some(bv))

        val uprn = "999999999999"

        when(mockAddressLookupService.getUprn(eqTo(baseAddress))(any()))
          .thenReturn(Future.successful(Some(uprn)))

        val expectedBV = bv.copy(
          registeredAddress = Some(baseAddress.copy(uprn = Some(uprn)))
        )

        when(
          mockJourneyAnswersService.update(
            eqTo(expectedBV),
            eqTo(testGroupId),
            eqTo(testProviderId)
          )(any[Writes[BusinessVerification]], any[HeaderCarrier])
        ).thenReturn(Future.successful(expectedBV))

        service.enrichUprnIfMissing(testGroupId, testProviderId, Some(journeyData)).futureValue

        verify(mockAddressLookupService).getUprn(baseAddress)

        verify(mockJourneyAnswersService).update(
          eqTo(expectedBV),
          eqTo(testGroupId),
          eqTo(testProviderId)
        )(any[Writes[BusinessVerification]], any[HeaderCarrier])
      }

      "must use default UPRN when lookup returns no uprn in response" in {

        val bv          = bvWith(Some(baseAddress))
        val journeyData = journeyDataWith(Some(bv))

        when(mockAddressLookupService.getUprn(eqTo(baseAddress))(any()))
          .thenReturn(Future.successful(Some(defaultUprn)))

        val expectedBV = bv.copy(
          registeredAddress = Some(baseAddress.copy(uprn = Some(defaultUprn)))
        )

        when(
          mockJourneyAnswersService.update(
            eqTo(expectedBV),
            eqTo(testGroupId),
            eqTo(testProviderId)
          )(any[Writes[BusinessVerification]], any[HeaderCarrier])
        ).thenReturn(Future.successful(expectedBV))

        service.enrichUprnIfMissing(testGroupId, testProviderId, Some(journeyData)).futureValue

        verify(mockJourneyAnswersService).update(any(), any(), any())(any(), any())
      }

      "must not persist when address lookup returns None (failure)" in {

        val bv          = bvWith(Some(baseAddress))
        val journeyData = journeyDataWith(Some(bv))

        when(mockAddressLookupService.getUprn(eqTo(baseAddress))(any()))
          .thenReturn(Future.successful(None))

        service.enrichUprnIfMissing(testGroupId, testProviderId, Some(journeyData)).futureValue

        verify(mockAddressLookupService).getUprn(baseAddress)
        verify(mockJourneyAnswersService, never()).update(any(), any(), any())(any(), any())
      }

      "must not fail when address lookup throws an exception" in {

        val bv          = bvWith(Some(baseAddress))
        val journeyData = journeyDataWith(Some(bv))

        when(mockAddressLookupService.getUprn(eqTo(baseAddress))(any()))
          .thenReturn(Future.failed(new RuntimeException("boom")))

        service.enrichUprnIfMissing(testGroupId, testProviderId, Some(journeyData)).futureValue

        verify(mockJourneyAnswersService, never()).update(any(), any(), any())(any(), any())
      }

      "must not fail when persistence fails" in {

        val bv          = bvWith(Some(baseAddress))
        val journeyData = journeyDataWith(Some(bv))

        val uprn = "999999999999"

        when(mockAddressLookupService.getUprn(eqTo(baseAddress))(any()))
          .thenReturn(Future.successful(Some(uprn)))

        val expectedBV = bv.copy(
          registeredAddress = Some(baseAddress.copy(uprn = Some(uprn)))
        )

        when(
          mockJourneyAnswersService.update(
            eqTo(expectedBV),
            any(),
            any()
          )(any[Writes[BusinessVerification]], any[HeaderCarrier])
        ).thenReturn(Future.failed(new RuntimeException("persist failed")))

        service.enrichUprnIfMissing(testGroupId, testProviderId, Some(journeyData)).futureValue

        verify(mockJourneyAnswersService).update(any(), any(), any())(any(), any())
      }
    }
  }
}
