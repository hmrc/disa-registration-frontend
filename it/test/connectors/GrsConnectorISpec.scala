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

import com.github.tomakehurst.wiremock.client.WireMock.*
import models.grs.*
import models.journeydata.RegisteredAddress
import play.api.http.Status.{CREATED, NOT_FOUND, OK, SERVICE_UNAVAILABLE}
import play.api.test.Helpers.await
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.BaseIntegrationSpec
import utils.WiremockHelper.{stubGet, stubPost}

import java.time.LocalDate

class GrsConnectorISpec extends BaseIntegrationSpec {

  private val journeyStartUrl            = "http://localhost/start"
  private val serviceUnavailableMessage = "Service unavailable"
  private val testCompanyName            = "Test Company Ltd"
  private val testCompanyNumber          = "01234567"
  private val testTaxIdentifier           = "1234567890"
  private val testBusinessPartnerId      = "X00000123456789"
  private val testPostcode               = "AA11AA"
  private val testAddressLine1            = "testLine1"
  private val testAddressLine2            = "test town"
  private val testAddressLine3            = "test city"
  private val registeredStatus            = "REGISTERED"
  private val passedVerificationStatus    = "PASS"

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
      regime = "DISA",
      accessibilityUrl = "/accessibility-statement/my-service",
      labels = None
    )

    "POST to the incorporated-entity-identification create journey URL for an incorporated entity company type" in {
      stubPost(
        "/incorporated-entity-identification/api/limited-company-journey",
        CREATED,
        s"""{ "journeyStartUrl": "$journeyStartUrl" }"""
      )

      val response = await(connector.createJourney(GrsCompanyType.LimitedCompany, requestBody))

      response shouldBe CreateJourneyResponse(journeyStartUrl)
    }

    "POST to the partnership-identification create journey URL for a partnership company type" in {
      stubPost(
        "/partnership-identification/api/general-partnership-journey",
        CREATED,
        s"""{ "journeyStartUrl": "$journeyStartUrl" }"""
      )

      val response = await(connector.createJourney(GrsCompanyType.GeneralPartnership, requestBody))

      response shouldBe CreateJourneyResponse(journeyStartUrl)
    }

    "retry when the backend returns a 5xx response" in {
      val path = "/incorporated-entity-identification/api/limited-company-journey"
      stubPost(path, SERVICE_UNAVAILABLE, serviceUnavailableMessage)

      val ex = await(connector.createJourney(GrsCompanyType.LimitedCompany, requestBody).failed)

      ex shouldBe a[UpstreamErrorResponse]
      verify(callAmountWithRetries, postRequestedFor(urlEqualTo(path)))
    }
  }

  "GrsConnector.fetchJourneyData" should {

    val testJourneyId = "testJourneyId"

    "return an IncorporatedEntityGRSResponse with correct fields when backend returns 200 OK" in {
      val fetchJourneyUrl = s"/incorporated-entity-identification/api/journey/$testJourneyId"

      val testGRSJsonResponse =
        s"""
          |{
          |  "companyProfile": {
          |    "companyName": "$testCompanyName",
          |    "companyNumber": "$testCompanyNumber",
          |    "dateOfIncorporation": "2020-01-01",
          |    "unsanitisedCHROAddress": {
          |      "address_line_1":"$testAddressLine1",
          |      "address_line_2":"$testAddressLine2",
          |      "care_of":"test name",
          |      "country":"United Kingdom",
          |      "locality":"$testAddressLine3",
          |      "po_box":"123",
          |      "postal_code":"$testPostcode",
          |      "premises":"1",
          |      "region":"test region"
          |    }
          |  },
          |  "identifiersMatch": true,
          |  "registration": {
          |    "registrationStatus": "$registeredStatus",
          |    "registeredBusinessPartnerId": "$testBusinessPartnerId"
          |  },
          |  "ctutr": "$testTaxIdentifier",
          |  "businessVerification": {
          |    "verificationStatus": "$passedVerificationStatus"
          |  }
          |}
          |""".stripMargin

      stubGet(fetchJourneyUrl, OK, testGRSJsonResponse)

      val response = await(connector.fetchJourneyData(GrsCompanyType.LimitedCompany, testJourneyId))

      response shouldBe IncorporatedEntityGRSResponse(
        companyNumber = Some(testCompanyNumber),
        companyName = Some(testCompanyName),
        ctutr = Some(testTaxIdentifier),
        dateOfIncorporation = Some(LocalDate.parse("2020-01-01")),
        identifiersMatch = true,
        businessRegistrationStatus = RegisteredStatus,
        businessVerificationStatus = Some(BvPass),
        bpSafeId = Some(testBusinessPartnerId),
        registeredAddress = Some(
          RegisteredAddress(
            addressLine1 = Some(testAddressLine1),
            addressLine2 = Some(testAddressLine2),
            addressLine3 = Some(testAddressLine3),
            postCode = Some(testPostcode)
          )
        )
      )
    }

    "return a PartnershipGRSResponse with correct fields when backend returns 200 OK" in {
      val fetchJourneyUrl = s"/partnership-identification/api/journey/$testJourneyId"

      val testGRSJsonResponse =
        s"""
          |{
          |  "sautr": "$testTaxIdentifier",
          |  "postcode": "$testPostcode",
          |  "identifiersMatch": true,
          |  "registration": {
          |    "registrationStatus": "$registeredStatus",
          |    "registeredBusinessPartnerId": "$testBusinessPartnerId"
          |  },
          |  "businessVerification": {
          |    "verificationStatus": "$passedVerificationStatus"
          |  }
          |}
          |""".stripMargin

      stubGet(fetchJourneyUrl, OK, testGRSJsonResponse)

      val response = await(connector.fetchJourneyData(GrsCompanyType.GeneralPartnership, testJourneyId))

      response shouldBe PartnershipGRSResponse(
        sautr = Some(testTaxIdentifier),
        saPostcode = Some(testPostcode),
        companyNumber = None,
        companyName = None,
        identifiersMatch = true,
        businessRegistrationStatus = RegisteredStatus,
        businessVerificationStatus = Some(BvPass),
        bpSafeId = Some(testBusinessPartnerId),
        registeredAddress = None
      )
    }

    "propagate exception when backend returns an error status (404)" in {
      val fetchJourneyUrl = s"/incorporated-entity-identification/api/journey/$testJourneyId"

      stubGet(fetchJourneyUrl, NOT_FOUND, """{"error":"Not Found"}""")

      val ex = intercept[UpstreamErrorResponse] {
        await(connector.fetchJourneyData(GrsCompanyType.LimitedCompany, testJourneyId))
      }

      ex.statusCode shouldBe NOT_FOUND
      ex.getMessage should include("Not Found")
    }

    "retry when the backend returns a 5xx response" in {
      val fetchJourneyUrl = s"/incorporated-entity-identification/api/journey/$testJourneyId"
      stubGet(fetchJourneyUrl, SERVICE_UNAVAILABLE, serviceUnavailableMessage)

      val ex = await(connector.fetchJourneyData(GrsCompanyType.LimitedCompany, testJourneyId).failed)

      ex shouldBe a[UpstreamErrorResponse]
      verify(callAmountWithRetries, getRequestedFor(urlEqualTo(fetchJourneyUrl)))
    }
  }
}
