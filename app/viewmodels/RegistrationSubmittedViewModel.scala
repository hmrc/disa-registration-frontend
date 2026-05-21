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

import config.FrontendAppConfig
import models.journeydata.JourneyData

final case class RegistrationSubmittedViewModel(
  formBundleId: String,
  contentKey: String,
  bulletKeys: Seq[String],
  showManageAccount: Boolean,
  businessTaxAccountUrl: String,
  guidanceUrl: String,
  feedbackUrl: String
) {

  private def contentPrefix: String =
    s"registrationSubmitted.$contentKey"

  val panelTitleKey: String =
    s"$contentPrefix.panel.title"

  val thankYouKey: String =
    s"$contentPrefix.thankYou"

  val letterIntroKey: String =
    s"$contentPrefix.letterIntro"

  val manageAccountHeadingKey: String =
    "registrationSubmitted.existing.manage.heading"

  val manageAccountParagraphKey: String =
    "registrationSubmitted.existing.manage.paragraph"

  val manageAccountLinkTextKey: String =
    "registrationSubmitted.existing.manage.paragraph.linkText"

  val guidanceParagraphKey: String =
    "registrationSubmitted.guidance.paragraph"

  val guidanceLinkTextKey: String =
    "registrationSubmitted.guidance.linkText"

  val feedbackTextKey: String =
    "registrationSubmitted.feedback.linkText"
}

object RegistrationSubmittedViewModel {

  private val Existing = "existing"
  private val New      = "new"

  def from(
    journeyData: JourneyData,
    appConfig: FrontendAppConfig
  ): Option[RegistrationSubmittedViewModel] =
    journeyData.formBundleId.map { formBundleId =>
      val existingIsaManager = isExistingIsaManager(journeyData)
      val contentKey         = if (existingIsaManager) Existing else New
      val bulletCount        = if (existingIsaManager) 2 else 3

      RegistrationSubmittedViewModel(
        formBundleId = formBundleId,
        contentKey = contentKey,
        bulletKeys = bulletKeys(contentKey, bulletCount),
        showManageAccount = existingIsaManager,
        businessTaxAccountUrl = appConfig.businessTaxAccountUrl,
        guidanceUrl = appConfig.isaManagersGuidanceUrl,
        feedbackUrl = appConfig.exitSurveyUrl
      )
    }

  private def isExistingIsaManager(journeyData: JourneyData): Boolean =
    journeyData.organisationDetails.flatMap(_.zRefNumber).isDefined

  private def bulletKeys(contentKey: String, count: Int): Seq[String] =
    1.to(count).map { index =>
      s"registrationSubmitted.$contentKey.bullet.$index"
    }
}
