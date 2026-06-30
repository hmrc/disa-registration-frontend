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

import base.SpecBase
import controllers.certificatesofauthority.routes.{CoaCheckYourAnswersController, EligibilityToManageIsaController}
import controllers.isaproducts.routes.{IsaProductsCheckYourAnswersController, IsaProductsController}
import controllers.liaisonofficers.routes.{AddedLiaisonOfficersController, LiaisonOfficerNameController}
import controllers.orgdetails.routes.{OrganisationDetailsCheckYourAnswersController, RegisteredIsaManagerController}
import controllers.orgemail.routes.{EmailVerificationCodeController, OrganisationEmailAddressController, OrganisationEmailCyaController}
import controllers.routes.SubmissionCyaController
import controllers.signatories.routes.{AddedSignatoryController, SignatoryNameController}
import controllers.thirdparty.routes.{AddedThirdPartiesController, ProductsManagedByThirdPartyController, ThirdPartiesCheckYourAnswersController}
import models.{CheckMode, NormalMode, YesNoAnswer}
import models.journeydata.certificatesofauthority.CertificatesOfAuthority
import models.journeydata.isaproducts.IsaProducts
import models.journeydata.liaisonofficers.LiaisonOfficers
import models.journeydata.signatories.Signatories
import models.journeydata.thirdparty.ThirdPartyOrganisations
import models.journeydata.{DeclareAndSubmit, JourneyData, OrganisationDetails, OrganisationEmail}
import play.api.mvc.Call
import uk.gov.hmrc.auth.core.{Assistant, CredentialRole, User}

class TaskListRoutesSpec extends SpecBase {

  "TaskListRoutes.destination" - {

    "must return None when the task list cannot be accessed" in {
      destination(OrganisationDetails.sectionName, emptyJourneyData) mustBe None
    }

    "must return None for an unknown section" in {
      destination("unknown-section", completeTaskListJourneyData) mustBe None
    }

    "must route organisation details to the start or check answers page" in {
      destinationCall(
        OrganisationDetails.sectionName,
        emptyJourneyDataWithBusinessVerification
      ) mustBe RegisteredIsaManagerController.onPageLoad(NormalMode)

      destinationCall(
        OrganisationDetails.sectionName,
        completeTaskListJourneyData
      ) mustBe OrganisationDetailsCheckYourAnswersController.onPageLoad()
    }

    "must route organisation email based on verification progress" in {
      destinationCall(
        OrganisationEmail.sectionName,
        completeTaskListJourneyData.copy(organisationEmail = None)
      ) mustBe OrganisationEmailAddressController.onPageLoad(NormalMode)

      destinationCall(
        OrganisationEmail.sectionName,
        completeTaskListJourneyData.copy(organisationEmail = Some(unverifiedTaskListOrganisationEmail))
      ) mustBe EmailVerificationCodeController.onPageLoad(NormalMode)

      destinationCall(
        OrganisationEmail.sectionName,
        completeTaskListJourneyData
      ) mustBe OrganisationEmailCyaController.onPageLoad()
    }

    "must route ISA products to the start or check answers page" in {
      destinationCall(
        IsaProducts.sectionName,
        completeTaskListJourneyData.copy(isaProducts = None)
      ) mustBe IsaProductsController.onPageLoad(NormalMode)

      destinationCall(
        IsaProducts.sectionName,
        completeTaskListJourneyData
      ) mustBe IsaProductsCheckYourAnswersController.onPageLoad()
    }

    "must route certificates of authority to the start or check answers page" in {
      destinationCall(
        CertificatesOfAuthority.sectionName,
        completeTaskListJourneyData.copy(certificatesOfAuthority = None)
      ) mustBe EligibilityToManageIsaController.onPageLoad()

      destinationCall(
        CertificatesOfAuthority.sectionName,
        completeTaskListJourneyData
      ) mustBe CoaCheckYourAnswersController.onPageLoad()
    }

    "must route liaison officers based on whether any records exist" in {
      destinationCall(
        LiaisonOfficers.sectionName,
        completeTaskListJourneyData.copy(liaisonOfficers = None)
      ) mustBe LiaisonOfficerNameController.onPageLoad(id = None, mode = NormalMode, returnTo = None)

      destinationCall(
        LiaisonOfficers.sectionName,
        completeTaskListJourneyData
      ) mustBe AddedLiaisonOfficersController.onPageLoad(NormalMode)
    }

    "must route signatories based on whether any records exist" in {
      destinationCall(
        Signatories.sectionName,
        completeTaskListJourneyData.copy(signatories = None)
      ) mustBe SignatoryNameController.onPageLoad(id = None, mode = NormalMode, returnTo = None)

      destinationCall(
        Signatories.sectionName,
        completeTaskListJourneyData
      ) mustBe AddedSignatoryController.onPageLoad(NormalMode)
    }

    "must route third party organisations based on progress" in {
      destinationCall(
        ThirdPartyOrganisations.sectionName,
        completeTaskListJourneyData.copy(thirdPartyOrganisations = None)
      ) mustBe ProductsManagedByThirdPartyController.onPageLoad(NormalMode)

      destinationCall(
        ThirdPartyOrganisations.sectionName,
        completeTaskListJourneyData.copy(thirdPartyOrganisations =
          Some(testThirdPartyOrganisations(Seq(inProgressTaskListThirdParty("tp-1"))))
        )
      ) mustBe AddedThirdPartiesController.onPageLoad(NormalMode)

      destinationCall(
        ThirdPartyOrganisations.sectionName,
        completeTaskListJourneyData.copy(thirdPartyOrganisations =
          Some(
            testThirdPartyOrganisations(
              Seq(
                completeTaskListThirdParty("tp-1"),
                completeTaskListThirdParty("tp-2")
              )
            )
          )
        )
      ) mustBe ThirdPartiesCheckYourAnswersController.onPageLoad()

      destinationCall(
        ThirdPartyOrganisations.sectionName,
        completeTaskListJourneyData.copy(thirdPartyOrganisations =
          Some(testThirdPartyOrganisations(Seq(completeTaskListThirdParty("tp-1"))))
        )
      ) mustBe AddedThirdPartiesController.onPageLoad(NormalMode)

      destinationCall(
        ThirdPartyOrganisations.sectionName,
        completeTaskListJourneyData.copy(thirdPartyOrganisations =
          Some(testThirdPartyOrganisations(Nil, managedByThirdParty = YesNoAnswer.No))
        )
      ) mustBe ProductsManagedByThirdPartyController.onPageLoad(CheckMode)
    }

    "must route declare and submit only when the user can submit" in {
      destinationCall(
        DeclareAndSubmit.sectionName,
        completeTaskListJourneyData
      ) mustBe SubmissionCyaController.onPageLoad()

      destination(DeclareAndSubmit.sectionName, completeTaskListJourneyData, Assistant) mustBe None
      destination(DeclareAndSubmit.sectionName, completeTaskListJourneyData.copy(organisationEmail = None)) mustBe None
    }
  }

  private def destination(
    sectionName: String,
    journeyData: JourneyData,
    credentialRole: CredentialRole = User
  ): Option[TaskListDestination] =
    TaskListRoutes.destination(sectionName, journeyData, credentialRole)

  private def destinationCall(
    sectionName: String,
    journeyData: JourneyData,
    credentialRole: CredentialRole = User
  ): Call = {
    val result = destination(sectionName, journeyData, credentialRole).value

    result.sectionName mustBe sectionName
    result.call
  }
}
