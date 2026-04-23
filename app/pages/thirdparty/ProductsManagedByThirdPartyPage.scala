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

import models.YesNoAnswer.{No, Yes}
import models.journeydata.thirdparty.ThirdPartyOrganisations
import pages.{ClearablePage, PageWithDependents}

case object ProductsManagedByThirdPartyPage extends PageWithDependents[ThirdPartyOrganisations] {

  // TODO Add pages as section is complete
  def pagesToClear(currentAnswers: ThirdPartyOrganisations): List[ClearablePage[ThirdPartyOrganisations]] = List.empty

  def resumeNormalMode(currentAnswers: ThirdPartyOrganisations): Boolean = {
    val yesAnswerWithNoDetails = currentAnswers.managedByThirdParty.contains(Yes) && currentAnswers.thirdParties.isEmpty
    val noOrMissingAnswer      = currentAnswers.managedByThirdParty.fold(true)(_ == No)

    yesAnswerWithNoDetails || noOrMissingAnswer
  }
}
