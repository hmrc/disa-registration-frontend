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

package viewmodels

import base.SpecBase
import models.journeydata.OrganisationDetails
import org.mockito.Mockito.when

class RegistrationSubmittedViewModelSpec extends SpecBase {

  private val subscriptionId        = "XEIS1234567890"
  private val businessTaxAccountUrl = "/business-tax-account"
  private val guidanceUrl           = "/isa-guidance"
  private val feedbackUrl           = "/feedback"

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(mockAppConfig.businessTaxAccountUrl).thenReturn(businessTaxAccountUrl)
    when(mockAppConfig.isaManagersGuidanceUrl).thenReturn(guidanceUrl)
    when(mockAppConfig.exitSurveyUrl).thenReturn(feedbackUrl)
  }

  "RegistrationSubmittedViewModel" - {

    ".from" - {

      "must return None when subscriptionId is missing" in {

        val journeyData = emptyJourneyData.copy(subscriptionId = None)

        val result = RegistrationSubmittedViewModel.from(journeyData, mockAppConfig)

        result mustBe None
      }

      "must return a view model for a new ISA manager application" in {

        val journeyData = emptyJourneyData.copy(
          subscriptionId = Some(subscriptionId),
          organisationDetails = None
        )

        val result = RegistrationSubmittedViewModel.from(journeyData, mockAppConfig).value

        result.subscriptionId mustEqual subscriptionId
        result.contentKey mustEqual "new"
        result.bulletKeys mustEqual Seq(
          "registrationSubmitted.new.bullet.1",
          "registrationSubmitted.new.bullet.2",
          "registrationSubmitted.new.bullet.3"
        )
        result.showManageAccount mustBe false
        result.businessTaxAccountUrl mustEqual businessTaxAccountUrl
        result.guidanceUrl mustEqual guidanceUrl
        result.feedbackUrl mustEqual feedbackUrl

        result.panelTitleKey mustEqual "registrationSubmitted.new.panel.title"
        result.thankYouKey mustEqual "registrationSubmitted.new.thankYou"
        result.letterIntroKey mustEqual "registrationSubmitted.new.letterIntro"

        result.manageAccountHeadingKey mustEqual "registrationSubmitted.existing.manage.heading"
        result.manageAccountParagraphKey mustEqual "registrationSubmitted.existing.manage.paragraph"
        result.manageAccountLinkTextKey mustEqual "registrationSubmitted.existing.manage.paragraph.linkText"

        result.guidanceParagraphKey mustEqual "registrationSubmitted.guidance.paragraph"
        result.guidanceLinkTextKey mustEqual "registrationSubmitted.guidance.linkText"
        result.feedbackTextKey mustEqual "registrationSubmitted.feedback.linkText"
      }

      "must return a view model for an existing ISA manager enrolment" in {

        val organisationDetails = mock[OrganisationDetails]
        when(organisationDetails.zRefNumber).thenReturn(Some(testZRef))

        val journeyData = emptyJourneyData.copy(
          subscriptionId = Some(subscriptionId),
          organisationDetails = Some(organisationDetails)
        )

        val result = RegistrationSubmittedViewModel.from(journeyData, mockAppConfig).value

        result.subscriptionId mustEqual subscriptionId
        result.contentKey mustEqual "existing"
        result.bulletKeys mustEqual Seq(
          "registrationSubmitted.existing.bullet.1",
          "registrationSubmitted.existing.bullet.2"
        )
        result.showManageAccount mustBe true
        result.businessTaxAccountUrl mustEqual businessTaxAccountUrl
        result.guidanceUrl mustEqual guidanceUrl
        result.feedbackUrl mustEqual feedbackUrl

        result.panelTitleKey mustEqual "registrationSubmitted.existing.panel.title"
        result.thankYouKey mustEqual "registrationSubmitted.existing.thankYou"
        result.letterIntroKey mustEqual "registrationSubmitted.existing.letterIntro"

        result.manageAccountHeadingKey mustEqual "registrationSubmitted.existing.manage.heading"
        result.manageAccountParagraphKey mustEqual "registrationSubmitted.existing.manage.paragraph"
        result.manageAccountLinkTextKey mustEqual "registrationSubmitted.existing.manage.paragraph.linkText"

        result.guidanceParagraphKey mustEqual "registrationSubmitted.guidance.paragraph"
        result.guidanceLinkTextKey mustEqual "registrationSubmitted.guidance.linkText"
        result.feedbackTextKey mustEqual "registrationSubmitted.feedback.linkText"
      }
    }
  }
}
