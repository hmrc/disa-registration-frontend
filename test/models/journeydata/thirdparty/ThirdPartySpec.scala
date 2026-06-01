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

package models.journeydata.thirdparty

import base.SpecBase
import models.YesNoAnswer

class ThirdPartySpec extends SpecBase {

  "ThirdParty.inProgress" - {

    "must return false when required answers are complete and investor funds are not used" in {
      val thirdParty =
        ThirdParty(
          id = "tp-1",
          thirdPartyName = Some("Third Party Ltd"),
          managingIsaReturns = Some(YesNoAnswer.No),
          usingInvestorFunds = Some(YesNoAnswer.No)
        )

      thirdParty.inProgress mustBe false
    }

    "must return false when investor funds are used and the percentage has been answered" in {
      val thirdParty =
        ThirdParty(
          id = "tp-1",
          thirdPartyName = Some("Third Party Ltd"),
          managingIsaReturns = Some(YesNoAnswer.No),
          usingInvestorFunds = Some(YesNoAnswer.Yes),
          investorFundsPercentage = Some("25")
        )

      thirdParty.inProgress mustBe false
    }

    "must return true when investor funds are used but the percentage has not been answered" in {
      val thirdParty =
        ThirdParty(
          id = "tp-1",
          thirdPartyName = Some("Third Party Ltd"),
          managingIsaReturns = Some(YesNoAnswer.No),
          usingInvestorFunds = Some(YesNoAnswer.Yes)
        )

      thirdParty.inProgress mustBe true
    }

    "must return true when a required answer is missing" in {
      val thirdParty =
        ThirdParty(
          id = "tp-1",
          thirdPartyName = Some("Third Party Ltd"),
          managingIsaReturns = None,
          usingInvestorFunds = Some(YesNoAnswer.No)
        )

      thirdParty.inProgress mustBe true
    }
  }
}
