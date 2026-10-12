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
import models.journeydata.{CorrespondenceAddress, JourneyData, OrganisationDetails}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.libs.json.Writes
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class CorrespondenceAddressUprnServiceSpec extends SpecBase {

  private val service    = new CorrespondenceAddressUprnService(mockAddressLookupService, mockJourneyAnswersService)
  private val providerId = "providerId"
  private val address    = CorrespondenceAddress(
    addressLine1 = Some("line1"),
    addressLine2 = Some("line2"),
    addressLine3 = Some("line3"),
    postCode = Some("AA1 1AA")
  )

  private def journeyDataWith(details: Option[OrganisationDetails]) =
    JourneyData(groupId = testGroupId, enrolmentId = testEnrolmentId, organisationDetails = details)

  "CorrespondenceAddressUprnService.enrichUprnIfMissing" - {
    "must do nothing when journey data is absent" in {
      service.enrichUprnIfMissing(testGroupId, providerId, None).futureValue shouldBe ((): Unit)

      verify(mockAddressLookupService, never()).getUprn(any())(any())
      verify(mockJourneyAnswersService, never()).update(any(), any(), any())(any(), any())
    }

    "must do nothing when organisation details are absent" in {
      service.enrichUprnIfMissing(testGroupId, providerId, Some(journeyDataWith(None))).futureValue

      verify(mockAddressLookupService, never()).getUprn(any())(any())
    }

    "must do nothing when correspondence address is absent" in {
      val details = OrganisationDetails(correspondenceAddress = None)
      service.enrichUprnIfMissing(testGroupId, providerId, Some(journeyDataWith(Some(details)))).futureValue

      verify(mockAddressLookupService, never()).getUprn(any())(any())
    }

    "must do nothing when the UPRN is already present" in {
      val details = OrganisationDetails(correspondenceAddress = Some(address.copy(uprn = Some("999"))))
      service.enrichUprnIfMissing(testGroupId, providerId, Some(journeyDataWith(Some(details)))).futureValue

      verify(mockAddressLookupService, never()).getUprn(any())(any())
      verify(mockJourneyAnswersService, never()).update(any(), any(), any())(any(), any())
    }

    "must look up and persist a missing UPRN" in {
      val details = OrganisationDetails(correspondenceAddress = Some(address))
      val updated = details.copy(correspondenceAddress = Some(address.copy(uprn = Some("999"))))
      when(mockAddressLookupService.getUprn(eqTo(address))(any())).thenReturn(Future.successful(Some("999")))
      when(
        mockJourneyAnswersService.update(eqTo(updated), eqTo(testGroupId), eqTo(providerId))(
          any[Writes[OrganisationDetails]],
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(updated))

      service.enrichUprnIfMissing(testGroupId, providerId, Some(journeyDataWith(Some(details)))).futureValue

      verify(mockJourneyAnswersService).update(eqTo(updated), eqTo(testGroupId), eqTo(providerId))(
        any[Writes[OrganisationDetails]],
        any[HeaderCarrier]
      )
    }

    "must persist the default UPRN returned by address lookup" in {
      val details = OrganisationDetails(correspondenceAddress = Some(address))
      val updated = details.copy(correspondenceAddress = Some(address.copy(uprn = Some(defaultUprn))))
      when(mockAddressLookupService.getUprn(eqTo(address))(any())).thenReturn(Future.successful(Some(defaultUprn)))
      when(mockJourneyAnswersService.update(eqTo(updated), any(), any())(any[Writes[OrganisationDetails]], any()))
        .thenReturn(Future.successful(updated))

      service.enrichUprnIfMissing(testGroupId, providerId, Some(journeyDataWith(Some(details)))).futureValue

      verify(mockJourneyAnswersService).update(eqTo(updated), any(), any())(any(), any())
    }

    "must not persist when lookup returns no UPRN" in {
      val details = OrganisationDetails(correspondenceAddress = Some(address))
      when(mockAddressLookupService.getUprn(eqTo(address))(any())).thenReturn(Future.successful(None))

      service.enrichUprnIfMissing(testGroupId, providerId, Some(journeyDataWith(Some(details)))).futureValue

      verify(mockJourneyAnswersService, never()).update(any(), any(), any())(any(), any())
    }

    "must remain non-blocking when lookup fails" in {
      val details = OrganisationDetails(correspondenceAddress = Some(address))
      when(mockAddressLookupService.getUprn(eqTo(address))(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.enrichUprnIfMissing(testGroupId, providerId, Some(journeyDataWith(Some(details)))).futureValue

      verify(mockJourneyAnswersService, never()).update(any(), any(), any())(any(), any())
    }

    "must remain non-blocking when persistence fails" in {
      val details = OrganisationDetails(correspondenceAddress = Some(address))
      when(mockAddressLookupService.getUprn(eqTo(address))(any())).thenReturn(Future.successful(Some("999")))
      when(mockJourneyAnswersService.update(any[OrganisationDetails], any(), any())(any(), any()))
        .thenReturn(Future.failed(new RuntimeException("persist failed")))

      service.enrichUprnIfMissing(testGroupId, providerId, Some(journeyDataWith(Some(details)))).futureValue

      verify(mockJourneyAnswersService).update(any[OrganisationDetails], any(), any())(any(), any())
    }
  }
}
