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

package models.journeydata

import uk.gov.hmrc.auth.core.{CredentialRole, User}

object TaskListProgress {

  def canAccessTaskList(journeyData: JourneyData): Boolean =
    journeyData.businessVerification.exists { businessVerification =>
      businessVerification.businessRegistrationPassed.contains(true) &&
      businessVerification.businessVerificationPassed.contains(true)
    }

  def canSubmitAnswers(journeyData: JourneyData, credentialRole: CredentialRole): Boolean =
    credentialRole == User && canAccessTaskList(journeyData) && allRequiredTasksComplete(journeyData)

  def allRequiredTasksComplete(journeyData: JourneyData): Boolean =
    journeyData.organisationDetails.exists(_.isComplete) &&
      journeyData.organisationEmail.exists(_.isComplete) &&
      journeyData.isaProducts.exists(_.isComplete) &&
      journeyData.certificatesOfAuthority.exists(_.isComplete) &&
      areLiaisonOfficersComplete(journeyData) &&
      areSignatoriesComplete(journeyData) &&
      journeyData.thirdPartyOrganisations.exists(_.isComplete)

  def areLiaisonOfficersComplete(journeyData: JourneyData): Boolean =
    journeyData.liaisonOfficers.exists { liaisonOfficers =>
      liaisonOfficers.liaisonOfficers.nonEmpty && liaisonOfficers.liaisonOfficers.forall(!_.inProgress)
    }

  def areSignatoriesComplete(journeyData: JourneyData): Boolean =
    journeyData.signatories.exists { signatories =>
      signatories.signatories.nonEmpty && signatories.signatories.forall(!_.inProgress)
    }

  private def nonEmpty(value: String): Boolean =
    value.trim.nonEmpty
}
