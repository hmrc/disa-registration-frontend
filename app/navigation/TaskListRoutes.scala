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

package navigation

import controllers.certificatesofauthority.routes.{CoaCheckYourAnswersController, EligibilityToManageIsaController}
import controllers.isaproducts.routes.{IsaProductsCheckYourAnswersController, IsaProductsController}
import controllers.liaisonofficers.routes.{AddedLiaisonOfficersController, LiaisonOfficerNameController}
import controllers.orgdetails.routes.{OrganisationDetailsCheckYourAnswersController, RegisteredIsaManagerController}
import controllers.orgemail.routes.{EmailVerificationCodeController, OrganisationEmailAddressController, OrganisationEmailCyaController}
import controllers.routes.SubmissionCyaController
import controllers.signatories.routes.{AddedSignatoryController, SignatoryNameController}
import controllers.thirdparty.routes.{AddedThirdPartiesController, ProductsManagedByThirdPartyController, ThirdPartiesCheckYourAnswersController}
import models.journeydata.certificatesofauthority.CertificatesOfAuthority
import models.journeydata.isaproducts.IsaProducts
import models.journeydata.liaisonofficers.LiaisonOfficers
import models.journeydata.signatories.Signatories
import models.journeydata.thirdparty.ThirdPartyOrganisations
import models.journeydata.{DeclareAndSubmit, JourneyData, OrganisationDetails, OrganisationEmail, TaskListProgress}
import models.{CheckMode, NormalMode}
import play.api.mvc.Call
import uk.gov.hmrc.auth.core.CredentialRole

final case class TaskListDestination(call: Call, sectionName: String)

object TaskListRoutes {

  def destination(
    sectionName: String,
    journeyData: JourneyData,
    credentialRole: CredentialRole
  ): Option[TaskListDestination] =
    destinationCall(sectionName, journeyData, credentialRole)
      .map(call => TaskListDestination(call, sectionName))

  private def destinationCall(
    sectionName: String,
    journeyData: JourneyData,
    credentialRole: CredentialRole
  ): Option[Call] =
    if (!TaskListProgress.canAccessTaskList(journeyData)) {
      None
    } else {
      sectionName match {
        case OrganisationDetails.sectionName =>
          Some(
            if (TaskListProgress.isOrganisationInformationComplete(journeyData))
              OrganisationDetailsCheckYourAnswersController.onPageLoad()
            else
              RegisteredIsaManagerController.onPageLoad(NormalMode)
          )

        case OrganisationEmail.sectionName =>
          organisationEmailDestination(journeyData)

        case IsaProducts.sectionName =>
          Some(
            if (TaskListProgress.isIsaProductsComplete(journeyData)) IsaProductsCheckYourAnswersController.onPageLoad()
            else IsaProductsController.onPageLoad(NormalMode)
          )

        case CertificatesOfAuthority.sectionName =>
          Some(
            if (TaskListProgress.isCertificatesOfAuthorityComplete(journeyData))
              CoaCheckYourAnswersController.onPageLoad()
            else
              EligibilityToManageIsaController.onPageLoad()
          )

        case LiaisonOfficers.sectionName =>
          Some(
            if (journeyData.liaisonOfficers.exists(_.liaisonOfficers.nonEmpty))
              AddedLiaisonOfficersController.onPageLoad(NormalMode)
            else
              LiaisonOfficerNameController.onPageLoad(id = None, mode = NormalMode, returnTo = None)
          )

        case Signatories.sectionName =>
          Some(
            if (journeyData.signatories.exists(_.signatories.nonEmpty))
              AddedSignatoryController.onPageLoad(NormalMode)
            else
              SignatoryNameController.onPageLoad(id = None, mode = NormalMode, returnTo = None)
          )

        case ThirdPartyOrganisations.sectionName =>
          thirdPartyOrganisationsDestination(journeyData)

        case DeclareAndSubmit.sectionName =>
          Option.when(TaskListProgress.canSubmitAnswers(journeyData, credentialRole)) {
            SubmissionCyaController.onPageLoad()
          }

        case _ =>
          None
      }
    }

  private def organisationEmailDestination(journeyData: JourneyData): Option[Call] = {
    val email    = journeyData.organisationEmail.flatMap(_.organisationEmail)
    val verified = TaskListProgress.isOrganisationEmailVerified(journeyData)

    Some(
      if (verified) OrganisationEmailCyaController.onPageLoad()
      else if (email.isDefined) EmailVerificationCodeController.onPageLoad(NormalMode)
      else OrganisationEmailAddressController.onPageLoad(NormalMode)
    )
  }

  private def thirdPartyOrganisationsDestination(journeyData: JourneyData): Option[Call] = {
    val section            = journeyData.thirdPartyOrganisations
    val count              = section.fold(0)(_.thirdParties.size)
    val complete           = TaskListProgress.areThirdPartyOrganisationsComplete(journeyData)
    val hasIncompleteItems = section.exists(_.thirdParties.exists(_.inProgress))

    Some(
      if (hasIncompleteItems) AddedThirdPartiesController.onPageLoad(NormalMode)
      else if (count > 1) ThirdPartiesCheckYourAnswersController.onPageLoad()
      else if (count > 0) AddedThirdPartiesController.onPageLoad(NormalMode)
      else if (complete) ProductsManagedByThirdPartyController.onPageLoad(CheckMode)
      else ProductsManagedByThirdPartyController.onPageLoad(NormalMode)
    )
  }
}
