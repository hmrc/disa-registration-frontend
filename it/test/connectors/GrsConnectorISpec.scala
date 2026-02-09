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
import play.api.test.Helpers.await
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.BaseIntegrationSpec
import utils.WiremockHelper.stubGet

import java.time.LocalDate

class GrsConnectorISpec extends BaseIntegrationSpec {

  override def config: Map[String, String] =
    super.config ++ Map(
      "features.use-grs-stub" -> "false"
    )

  val connector: GrsConnector = app.injector.instanceOf[GrsConnector]
  
  "GrsConnector.fetchJourneyData" should {

    val testJourneyId = "testJourneyId"
    val fetchJourneyUrl =
      s"/incorporated-entity-identification/api/journey/$testJourneyId"

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
  }
}
