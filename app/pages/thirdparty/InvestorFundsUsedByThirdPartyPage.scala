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

package pages.thirdparty

import models.YesNoAnswer
import models.journeydata.thirdparty.ThirdPartyOrganisations
import pages.{ClearablePage, PageWithDependents}

case class InvestorFundsUsedByThirdPartyPage(id: String) extends PageWithDependents[ThirdPartyOrganisations] {
  override def pagesToClear(
    currentAnswers: ThirdPartyOrganisations
  ): List[ClearablePage[ThirdPartyOrganisations]] =
    currentAnswers.thirdParties.collect {
      case tp if tp.id == id && tp.usingInvestorFunds.contains(YesNoAnswer.No) =>
        ThirdPartyInvestorFundsPercentagePage(id)
    }.toList

  override def resumeNormalMode(currentAnswers: ThirdPartyOrganisations): Boolean =
    currentAnswers.thirdParties.exists { tp =>
      tp.id == id &&
      (tp.usingInvestorFunds.contains(YesNoAnswer.Yes) && tp.investorFundsPercentage.isEmpty)
    }
}
