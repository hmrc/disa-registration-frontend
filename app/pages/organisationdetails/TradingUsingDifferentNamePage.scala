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

package pages.organisationdetails

import models.YesNoAnswer
import models.YesNoAnswer.Yes
import models.journeydata.OrganisationDetails
import pages.{ClearablePage, PageWithDependents}

case object TradingUsingDifferentNamePage extends PageWithDependents[OrganisationDetails] {

  override def toString: String = "tradingUsingDifferentName"

  def clearAnswer(answers: OrganisationDetails): OrganisationDetails =
    answers.copy(tradingUsingDifferentName = None)

  override def pagesToClear(
    currentAnswers: OrganisationDetails
  ): List[ClearablePage[OrganisationDetails]] = {
    val shouldClear =
      currentAnswers.tradingUsingDifferentName.contains(YesNoAnswer.No) &&
        currentAnswers.tradingName.nonEmpty

    if (shouldClear) {
      List(
        new ClearablePage[OrganisationDetails] {
          override def clearAnswer(section: OrganisationDetails): OrganisationDetails =
            section.copy(tradingName = None)
        }
      )
    } else {
      Nil
    }
  }

  def resumeNormalMode(currentAnswers: OrganisationDetails): Boolean = {
    val yesAnswerWithNoDetails =
      currentAnswers.tradingUsingDifferentName.contains(Yes) && currentAnswers.tradingName.isEmpty
    val noOrMissingAnswer      = currentAnswers.tradingUsingDifferentName.fold(false)(_ == YesNoAnswer.No)
    yesAnswerWithNoDetails || noOrMissingAnswer
  }
}
