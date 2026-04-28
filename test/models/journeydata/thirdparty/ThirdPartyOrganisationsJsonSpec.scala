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

package models.journeydata.thirdparties

import models.YesNoAnswer
import models.YesNoAnswer.Yes
import models.journeydata.thirdparty.{ThirdParty, ThirdPartyOrganisations}
import play.api.libs.json.*
import utils.JsonFormatSpec

class ThirdPartyOrganisationsJsonSpec extends JsonFormatSpec[ThirdPartyOrganisations] {

  override val model: ThirdPartyOrganisations =
    ThirdPartyOrganisations(
      managedByThirdParty = Some(Yes),
      thirdParties = Seq(
        ThirdParty(
          id = "tp-1",
          thirdPartyName = Some("Test Org"),
          thirdPartyFrn = Some("123456"),
          managingIsaReturns = Some(YesNoAnswer.Yes),
          usingInvestorFunds = Some(YesNoAnswer.Yes),
          investorFundsPercentage = Some(10)
        )
      ),
      connectedOrganisations = Set("org-1", "org-2")
    )

  override val expectedJsonFromWrites: JsValue =
    Json.obj(
      "managedByThirdParty"    -> "yes",
      "thirdParties"           -> Json.arr(
        Json.obj(
          "id"                      -> "tp-1",
          "thirdPartyName"          -> "Test Org",
          "thirdPartyFrn"           -> "123456",
          "managingIsaReturns"      -> s"${YesNoAnswer.Yes}",
          "usingInvestorFunds"      -> s"${YesNoAnswer.Yes}",
          "investorFundsPercentage" -> 10
        )
      ),
      "connectedOrganisations" -> Json.arr("org-1", "org-2")
    )

  override implicit val format: Format[ThirdPartyOrganisations] =
    ThirdPartyOrganisations.format
}
