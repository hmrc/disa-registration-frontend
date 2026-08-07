/*
 * Copyright 2025 HM Revenue & Customs
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
import config.FrontendAppConfig
import connectors.GrsConnector
import models.grs.*
import models.journeydata.RegisteredAddress
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class GrsServiceSpec extends SpecBase {

  val mockConnector: GrsConnector  = mock[GrsConnector]
  val mockMessagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val service                      = new GrsService(mockConnector, app.injector.instanceOf[FrontendAppConfig], mockMessagesApi)

  implicit val request: Request[AnyContent] = FakeRequest()

  private val companyType = GrsCompanyType.LimitedCompany

  "GrsService" - {

    "getGRSJourneyStartUrl" - {

      "must return the journeyStartUrl when connector returns CreateJourneyResponse" in {
        val testResponse = CreateJourneyResponse("http://test-url.com")

        when(mockConnector.createJourney(any[GrsCompanyType], any[GrsCreateJourneyRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(testResponse))

        val result = service.getGRSJourneyStartUrl(companyType).futureValue

        result shouldBe "http://test-url.com"
        verify(mockConnector).createJourney(any[GrsCompanyType], any[GrsCreateJourneyRequest])(any[HeaderCarrier])
      }

      "must propagate exception from connector" in {
        val ex              = new Exception("GRS failed")
        val expectedRequest = ArgumentMatchers.any[GrsCreateJourneyRequest]

        when(mockConnector.createJourney(any[GrsCompanyType], expectedRequest)(any[HeaderCarrier]))
          .thenReturn(Future.failed(ex))

        val thrown = service.getGRSJourneyStartUrl(companyType).failed.futureValue

        thrown shouldBe ex
      }
    }

    "fetchGRSJourneyData" - {
      val testJourneyId   = "testJourneyId"
      val testGRSResponse = IncorporatedEntityGRSResponse(
        companyNumber = Some("01234567"),
        companyName = Some("Test Company Ltd"),
        ctutr = Some("1234567890"),
        dateOfIncorporation = Some(java.time.LocalDate.parse("2020-01-01")),
        identifiersMatch = true,
        businessRegistrationStatus = RegisteredStatus,
        businessVerificationStatus = Some(BvPass),
        bpSafeId = Some("X00000123456789"),
        registeredAddress = Some(
          RegisteredAddress(
            addressLine1 = Some("address line 1"),
            addressLine2 = Some("address line 2"),
            addressLine3 = Some("address line 3"),
            postCode = Some("postcode")
          )
        )
      )

      "must return GRSResponse when connector returns it successfully" in {
        when(
          mockConnector.fetchJourneyData(ArgumentMatchers.eq(companyType), ArgumentMatchers.eq(testJourneyId))(any())
        )
          .thenReturn(Future.successful(testGRSResponse))

        val result = service.fetchGRSJourneyData(companyType, testJourneyId).futureValue

        result shouldBe testGRSResponse
        verify(mockConnector).fetchJourneyData(companyType, testJourneyId)
      }

      "must propagate exception from connector" in {
        val ex = new Exception("Fetch failed")

        when(
          mockConnector.fetchJourneyData(ArgumentMatchers.eq(companyType), ArgumentMatchers.eq(testJourneyId))(any())
        )
          .thenReturn(Future.failed(ex))

        val thrown = service.fetchGRSJourneyData(companyType, testJourneyId).failed.futureValue

        thrown shouldBe ex
      }
    }
  }
}
