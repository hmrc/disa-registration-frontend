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

package connectors

import models.grs.*
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.BaseIntegrationSpec
import utils.WiremockHelper.{stubGet, stubPost}

import java.time.LocalDate

class GrsConnectorISpec extends BaseIntegrationSpec {

  val testGroupId = "123456"

  val connector: GrsConnector = app.injector.instanceOf[GrsConnector]

  "GrsConnector.createJourney" should {

    val testJourneyRequest = GrsCreateJourneyRequest(
      continueUrl = "http://localhost/continue",
      businessVerificationCheck = true,
      deskProServiceId = "deskProId",
      signOutUrl = "/some/url",
      regime = "ISA",
      accessibilityUrl = "/some/url",
      labels = Some(Labels(en = Some(ServiceLabel("serviceLabel"))))
    )
    val createJourneyUrl = "/incorporated-entity-identification/api/limited-company-journey"
    val testCreateJourneyResponse = CreateJourneyResponse("http://localhost/journey-link")

    "return CreateJourneyResponse when backend returns 200 OK" in {
      stubPost(createJourneyUrl, 200, Json.toJson(testCreateJourneyResponse).toString)

      val response = await(connector.createJourney(testJourneyRequest))

      response shouldBe testCreateJourneyResponse
    }

    "propagate exception when backend returns an error status (401)" in {
      stubPost(createJourneyUrl, 401, """{"error":"Unauthorized"}""")

      val ex = intercept[UpstreamErrorResponse] {
        await(connector.createJourney(testJourneyRequest))
      }

      ex.statusCode shouldBe 401
      ex.getMessage should include("Unauthorized")
    }

    "propagate exception when call fails with bad JSON" in {
      stubPost(createJourneyUrl, 200, """{"unexpected":"json"}""")

      val err = await(connector.createJourney(testJourneyRequest).failed)

      err shouldBe a[Exception]
    }
  }

  "GrsConnector.fetchJourneyData" should {

    val testJourneyId = "testJourneyId"
    val fetchJourneyUrl = s"/identify-your-incorporated-business/test-only/retrieve-journey?journeyId=$testJourneyId"

    val testGRSJsonResponse =
      """
        |{
        |  "companyProfile": {
        |    "companyName": "Test Company Ltd",
        |    "companyNumber": "01234567",
        |    "dateOfIncorporation": "2020-01-01",
        |    "unsanitisedCHROAddress": {
        |      "addressLine1": "1 Test Street",
        |      "addressLine2": "Test Area",
        |      "locality": "Test Town",
        |      "postalCode": "TE57 1NG",
        |      "country": "GB"
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

    "return GRSResponse with correct fields when backend returns 200 OK" in {
      stubGet(fetchJourneyUrl, 200, testGRSJsonResponse)

      val response = await(connector.fetchJourneyData(testJourneyId))

      response shouldBe GRSResponse(
        companyNumber = "01234567",
        companyName = Some("Test Company Ltd"),
        ctutr = Some("1234567890"),
        chrn = None,
        dateOfIncorporation = Some(LocalDate.parse("2020-01-01")),
        countryOfIncorporation = "GB",
        identifiersMatch = true,
        businessRegistrationStatus = RegisteredStatus,
        businessVerificationStatus = Some(BvPass),
        bpSafeId = Some("X00000123456789")
      )
    }

    "propagate exception when backend returns an error status (404)" in {
      stubGet(fetchJourneyUrl, 404, """{"error":"Not Found"}""")

      val ex = intercept[UpstreamErrorResponse] {
        await(connector.fetchJourneyData(testJourneyId))
      }

      ex.statusCode shouldBe 404
      ex.getMessage should include("Not Found")
    }

    "propagate exception when call fails with bad JSON" in {
      stubGet(fetchJourneyUrl, 200, """{"unexpected":"json"}""")

      val err = await(connector.fetchJourneyData(testJourneyId).failed)

      err shouldBe a[Exception]
    }
  }
}
