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

package viewmodels.checkAnswers.orgDetails

import controllers.orgdetails.routes.EnterYourOrganisationAddressController
import models.CheckMode
import models.ReturnTo.OrganisationDetailsCya
import models.journeydata.JourneyData
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object AddedCorrespondenceAddressSummary {

  def row(answers: JourneyData)(implicit messages: Messages): Option[SummaryListRow] =
    answers.organisationDetails.flatMap(_.correspondenceAddress).map { address =>

      val formattedAddress = Seq(
        address.addressLine1,
        address.addressLine2,
        address.addressLine3,
        address.postCode
      ).flatten
        .filter(_.nonEmpty)
        .map(HtmlFormat.escape)
        .mkString("<br>")

      SummaryListRowViewModel(
        key = "enterYourOrganisationAddress.checkYourAnswersLabel",
        value = ValueViewModel(HtmlContent(formattedAddress)),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            EnterYourOrganisationAddressController.onPageLoad(CheckMode, Some(OrganisationDetailsCya)).url
          ).withVisuallyHiddenText(
            messages("enterYourOrganisationAddress.change.hidden")
          )
        )
      )
    }
}
