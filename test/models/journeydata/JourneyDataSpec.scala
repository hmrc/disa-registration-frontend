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
import models.journeydata.certificatesofauthority.FcaArticles.Article14
import models.journeydata.certificatesofauthority.FinancialOrganisation.Bank
import models.journeydata.isaproducts.{IsaProduct, IsaProducts}
import models.journeydata.liaisonofficers.LiaisonOfficerCommunication.ByEmail
import models.journeydata.liaisonofficers.{LiaisonOfficer, LiaisonOfficers}
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
          ctUtr = Some("12345678"),
          registeredAddress = Some(
            RegisteredAddress(
              addressLine1 = Some("address line 1"),
              addressLine2 = Some("address line 2"),
              addressLine3 = Some("address line 3"),
              postCode = Some("post code"),
              uprn = None
            )
          )
        )
      ),
      isaProducts = Some(IsaProducts(Some(IsaProduct.values), Some(testString), Some(testString))),
      organisationDetails = Some(
        OrganisationDetails(
          registeredToManageIsa = Some(true),
          zRefNumber = Some("Z1"),
          tradingUsingDifferentName = Some(true),
          tradingName = Some(testString),
          fcaNumber = Some(testString),
          correspondenceAddress = None,
          orgTelephoneNumber = None
        )
      ),
      certificatesOfAuthority = Some(
        CertificatesOfAuthority(
          certificatesYesNo = Some(Yes),
          fcaArticles = Some(Seq(Article14)),
          financialOrganisation = Some(Seq(Bank))
        )
      ),
      liaisonOfficers =
        Some(LiaisonOfficers(Seq(LiaisonOfficer(testString, Some(testString), Some(testString), Set(ByEmail))))),
      signatories = None,
      outsourcedAdministration = Some(OutsourcedAdministration(Some("O1"), Some("O2"))),
      feesCommissionsAndIncentives = Some(FeesCommissionsAndIncentives(Some("F1"), Some("F2")))
    )

  override val expectedJsonFromWrites: JsValue = Json.parse(s"""
    {
      "groupId": "$testGroupId",
      "enrolmentId": "$testEnrolmentId",
      "businessVerification": {
        "businessRegistrationPassed": true,
        "businessVerificationPassed": true,
        "ctUtr": "12345678",
        "registeredAddress": {
          "addressLine1": "address line 1",
          "addressLine2": "address line 2",
          "addressLine3": "address line 3",
          "postCode": "post code"
        }
      },
      "organisationDetails": {
        "registeredToManageIsa": true,
        "zRefNumber": "Z1",
        "tradingUsingDifferentName": true,
        "tradingName": "test",
        "fcaNumber": "test"
      },
      "isaProducts": {
        "isaProducts": [
          "cashIsas",
          "cashJuniorIsas",
          "stocksAndSharesIsas",
          "stocksAndSharesJuniorIsas",
          "innovativeFinanceIsas"
        ],
        "p2pPlatform": "test",
        "p2pPlatformNumber": "test"
      },
      "certificatesOfAuthority": {
        "certificatesYesNo": "yes",
        "fcaArticles": ["article14"],
        "financialOrganisation": ["bank"]
      },
      "liaisonOfficers": {
        "liaisonOfficers":[{"id":"test","fullName":"test","phoneNumber":"test","communication":["byEmail"]}]
      },
      "outsourcedAdministration": {
        "dataItem": "O1",
        "dataItem2": "O2"
      },
      "feesCommissionsAndIncentives": {
        "dataItem": "F1",
        "dataItem2": "F2"
      }
    }
  """)

  override val incomingJsonToRead: JsValue = Json.parse(s"""
  {
    "groupId": "$testGroupId",
    "businessVerification": {
      "businessRegistrationPassed": true,
      "businessVerificationPassed": true,
      "ctUtr": "12345678",
      "registeredAddress": {
        "addressLine1": "address line 1",
        "addressLine2": "address line 2",
        "addressLine3": "address line 3",
        "postCode": "post code",
        "uprn": null
      }
    },
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
      "isaProducts": [
        "cashIsas",
        "cashJuniorIsas",
        "stocksAndSharesIsas",
        "stocksAndSharesJuniorIsas",
        "innovativeFinanceIsas"
      ],
      "p2pPlatform": "test",
      "p2pPlatformNumber": "test"
    },
    "certificatesOfAuthority": {
      "certificatesYesNo": "yes",
      "fcaArticles": ["article14"],
      "financialOrganisation": ["bank"]
    },
    "liaisonOfficers": {
      "liaisonOfficers":[{"id":"test","fullName":"test","phoneNumber":"test","communication":["byEmail"]}]
    },
    "outsourcedAdministration": {
      "dataItem": "O1",
      "dataItem2": "O2"
    },
    "feesCommissionsAndIncentives": {
      "dataItem": "F1",
      "dataItem2": "F2"
    },
    "lastUpdated": "2025-10-21T10:00:00Z"
  }
  """)

  override implicit val format: Format[JourneyData] = JourneyData.format
}
