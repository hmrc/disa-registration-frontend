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

package models.journeydata

import models.journeydata.certificatesofauthority.CertificatesOfAuthority
import models.journeydata.certificatesofauthority.CertificatesOfAuthorityYesNo.Yes
import models.journeydata.isaproducts.{IsaProduct, IsaProducts}
import play.api.libs.json.{Format, JsValue, Json}
import utils.JsonFormatSpec

class JourneyDataSpec extends JsonFormatSpec[JourneyData] {

  override val model: JourneyData =
    JourneyData(
      enrolmentId = testEnrolmentId,
      groupId = testGroupId,
      businessVerification = Some(
        BusinessVerification(
          businessRegistrationPassed = Some(true),
          businessVerificationPassed = Some(true),
          ctUtr = Some("12345678")
        )
      ),
      isaProducts = Some(IsaProducts(Some(IsaProduct.values), Some(testString), Some(testString))),
      organisationDetails =
        Some(OrganisationDetails(Some(true), Some("Z1"), Some(true), Some(testString), Some(testString), None)),
      certificatesOfAuthority = Some(CertificatesOfAuthority(Some(Yes))),
      liaisonOfficers = Some(LiaisonOfficers(Some("L"), Some("LO"))),
      signatories = None,
      outsourcedAdministration = Some(OutsourcedAdministration(Some("O1"), Some("O2"))),
      feesCommissionsAndIncentives = Some(FeesCommissionsAndIncentives(Some("F1"), Some("F2")))
    )

  override val expectedJsonFromWrites: JsValue = Json.parse(s"""
    {
      "groupId": "$testGroupId",
      "businessVerification": { "businessRegistrationPassed": true, "businessVerificationPassed": true, "ctUtr": "12345678"},
      "enrolmentId": "$testEnrolmentId",
      "organisationDetails": {
        "registeredToManageIsa": true,
        "zRefNumber": "Z1",
        "tradingUsingDifferentName": true,
        "tradingName": "test",
        "fcaNumber": "test"
      },
      "isaProducts": {
        "isaProducts" : ["cashIsas","cashJuniorIsas","stocksAndSharesIsas","stocksAndSharesJuniorIsas","innovativeFinanceIsas"],
        "p2pPlatform": "test",
        "p2pPlatformNumber": "test"
      },
      "certificatesOfAuthority": { "certificatesYesNo":"yes" },
      "liaisonOfficers": { "dataItem": "L", "dataItem2": "LO" },
      "outsourcedAdministration": { "dataItem": "O1", "dataItem2": "O2" },
      "feesCommissionsAndIncentives": { "dataItem": "F1", "dataItem2": "F2" }
    }
  """)

  override val incomingJsonToRead: JsValue = Json.parse(s"""
      {
        "groupId": "$testGroupId",
        "businessVerification": { "businessRegistrationPassed": true, "businessVerificationPassed": true, "ctUtr": "12345678"},
        "enrolmentId": "$testEnrolmentId",
        "status": "Active",
        "organisationDetails": {
          "registeredToManageIsa": true,
          "zRefNumber": "Z1",
          "tradingUsingDifferentName": true,
          "tradingName": "test",
          "fcaNumber": "test"
        },
        "isaProducts": {
          "isaProducts" : ["cashIsas","cashJuniorIsas","stocksAndSharesIsas","stocksAndSharesJuniorIsas","innovativeFinanceIsas"],
          "p2pPlatform": "test",
          "p2pPlatformNumber": "test"
        },
        "certificatesOfAuthority": { "certificatesYesNo":"yes" },
        "liaisonOfficers": { "dataItem": "L", "dataItem2": "LO" },
        "outsourcedAdministration": { "dataItem": "O1", "dataItem2": "O2" },
        "feesCommissionsAndIncentives": { "dataItem": "F1", "dataItem2": "F2" },
        "lastUpdated": "2025-10-21T10:00:00Z"
      }
    """)

  override implicit val format: Format[JourneyData] = JourneyData.format
}
