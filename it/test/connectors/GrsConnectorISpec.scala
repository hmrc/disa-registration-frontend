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

import models.grs.*
import models.journeydata.RegisteredAddress
import play.api.test.Helpers.await
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.BaseIntegrationSpec
import utils.WiremockHelper.{stubGet, stubPost}

import java.time.LocalDate

class GrsConnectorISpec extends BaseIntegrationSpec {

  override def config: Map[String, String] =
    super.config ++ Map(
      "features.use-grs-stub" -> "false"
    )

  val connector: GrsConnector = app.injector.instanceOf[GrsConnector]

  "GrsConnector.createJourney" should {

    val requestBody = GrsCreateJourneyRequest(
      continueUrl = "/some/continue/url",
      businessVerificationCheck = true,
      deskProServiceId = "deskProServiceId",
      signOutUrl = "/some/sign-out-url",
      regime = "ISA",
      accessibilityUrl = "/accessibility-statement/my-service",
      labels = None
    )

    "POST to the incorporated-entity-identification create journey URL for an incorporated entity company type" in {
      stubPost(
        "/incorporated-entity-identification/api/limited-company-journey",
        201,
        """{ "journeyStartUrl": "http://localhost/start" }"""
      )

      val response = await(connector.createJourney(GrsCompanyType.LimitedCompany, requestBody))

      response shouldBe CreateJourneyResponse("http://localhost/start")
    }

    "POST to the partnership-identification create journey URL for a partnership company type" in {
      stubPost(
        "/partnership-identification/api/general-partnership-journey",
        201,
        """{ "journeyStartUrl": "http://localhost/start" }"""
      )

      val response = await(connector.createJourney(GrsCompanyType.GeneralPartnership, requestBody))

      response shouldBe CreateJourneyResponse("http://localhost/start")
    }
  }

  "GrsConnector.fetchJourneyData" should {

    val testJourneyId = "testJourneyId"

    "return an IncorporatedEntityGRSResponse with correct fields when backend returns 200 OK" in {
      val fetchJourneyUrl = s"/incorporated-entity-identification/api/journey/$testJourneyId"

      val testGRSJsonResponse =
        """
          |{
          |  "companyProfile": {
          |    "companyName": "Test Company Ltd",
          |    "companyNumber": "01234567",
          |    "dateOfIncorporation": "2020-01-01",
          |    "unsanitisedCHROAddress": {
          |      "address_line_1":"testLine1",
          |      "address_line_2":"test town",
          |      "care_of":"test name",
          |      "country":"United Kingdom",
          |      "locality":"test city",
          |      "po_box":"123",
          |      "postal_code":"AA11AA",
          |      "premises":"1",
          |      "region":"test region"
          |    }
          |  },
          |  "identifiersMatch": true,
          |  "registration": {
          |    "registrationStatus": "REGISTERED",
          |    "registeredBusinessPartnerId": "X00000123456789"
          |  },
          |  "ctutr": "1234567890",
          |  "businessVerification": {
          |    "verificationStatus": "PASS"
          |  }
          |}
          |""".stripMargin

      stubGet(fetchJourneyUrl, 200, testGRSJsonResponse)

      val response = await(connector.fetchJourneyData(GrsCompanyType.LimitedCompany, testJourneyId))

      response shouldBe IncorporatedEntityGRSResponse(
        companyNumber = Some("01234567"),
        companyName = Some("Test Company Ltd"),
        ctutr = Some("1234567890"),
        dateOfIncorporation = Some(LocalDate.parse("2020-01-01")),
        identifiersMatch = true,
        businessRegistrationStatus = RegisteredStatus,
        businessVerificationStatus = Some(BvPass),
        bpSafeId = Some("X00000123456789"),
        registeredAddress = Some(
          RegisteredAddress(
            addressLine1 = Some("testLine1"),
            addressLine2 = Some("test town"),
            addressLine3 = Some("test city"),
            postCode = Some("AA11AA"),
            uprn = None
          )
        )
      )
    }

    "return a PartnershipGRSResponse with correct fields when backend returns 200 OK" in {
      val fetchJourneyUrl = s"/partnership-identification/api/journey/$testJourneyId"

      val testGRSJsonResponse =
        """
          |{
          |  "sautr": "1234567890",
          |  "postcode": "AA11AA",
          |  "identifiersMatch": true,
          |  "registration": {
          |    "registrationStatus": "REGISTERED",
          |    "registeredBusinessPartnerId": "X00000123456789"
          |  },
          |  "businessVerification": {
          |    "verificationStatus": "PASS"
          |  }
          |}
          |""".stripMargin

      stubGet(fetchJourneyUrl, 200, testGRSJsonResponse)

      val response = await(connector.fetchJourneyData(GrsCompanyType.GeneralPartnership, testJourneyId))

      response shouldBe PartnershipGRSResponse(
        sautr = Some("1234567890"),
        saPostcode = Some("AA11AA"),
        companyNumber = None,
        companyName = None,
        identifiersMatch = true,
        businessRegistrationStatus = RegisteredStatus,
        businessVerificationStatus = Some(BvPass),
        bpSafeId = Some("X00000123456789"),
        registeredAddress = None
      )
    }

    "propagate exception when backend returns an error status (404)" in {
      val fetchJourneyUrl = s"/incorporated-entity-identification/api/journey/$testJourneyId"

      stubGet(fetchJourneyUrl, 404, """{"error":"Not Found"}""")

      val ex = intercept[UpstreamErrorResponse] {
        await(connector.fetchJourneyData(GrsCompanyType.LimitedCompany, testJourneyId))
      }

      ex.statusCode shouldBe 404
      ex.getMessage should include("Not Found")
    }
  }
}
