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

import models.journeydata.isaproducts.{IsaProduct, IsaProducts}
import play.api.libs.json.{Format, JsValue, Json}
import utils.JsonFormatSpec

import java.time.Instant

class JourneyDataSpec extends JsonFormatSpec[JourneyData] {

  val instant: Instant = Instant.parse("2024-01-01T12:00:00Z")

  override val model: JourneyData =
    JourneyData(
      groupId = testGroupId,
      businessVerification = Some(BusinessVerification(Some("A"), Some("B"))),
      isaProducts = Some(IsaProducts(Some(IsaProduct.values), Some(testString), Some(testString))),
      organisationDetails =
        Some(OrganisationDetails(Some(true), Some("Z1"), Some(true), Some(testString), Some(testString), None)),
      certificatesOfAuthority = Some(CertificatesOfAuthority(Some("C"), Some("D"))),
      liaisonOfficers = Some(LiaisonOfficers(Some("L"), Some("LO"))),
      signatories = None,
      outsourcedAdministration = Some(OutsourcedAdministration(Some("O1"), Some("O2"))),
      feesCommissionsAndIncentives = Some(FeesCommissionsAndIncentives(Some("F1"), Some("F2")))
    )

  override val json: JsValue = Json.parse("""
    {
      "groupId": "id",
      "businessVerification": { "dataItem": "A", "dataItem2": "B" },
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
      "certificatesOfAuthority": { "dataItem": "C", "dataItem2": "D" },
      "liaisonOfficers": { "dataItem": "L", "dataItem2": "LO" },
      "outsourcedAdministration": { "dataItem": "O1", "dataItem2": "O2" },
      "feesCommissionsAndIncentives": { "dataItem": "F1", "dataItem2": "F2" }
    }
  """)

  override implicit val format: Format[JourneyData] = JourneyData.format
}
