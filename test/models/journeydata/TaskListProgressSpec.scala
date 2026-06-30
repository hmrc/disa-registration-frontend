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

import base.SpecBase
import models.YesNoAnswer
import models.journeydata.certificatesofauthority.CertificatesOfAuthority
import models.journeydata.certificatesofauthority.CertificatesOfAuthorityYesNo.{No as CertificatesNo, Yes as CertificatesYes}
import models.journeydata.certificatesofauthority.FcaArticles.Article14
import models.journeydata.certificatesofauthority.FinancialOrganisation.EuropeanInstitution
import models.journeydata.isaproducts.InnovativeFinancialProduct.PeertopeerLoansUsingAPlatformWith36hPermissions
import models.journeydata.isaproducts.IsaProduct.InnovativeFinanceIsas
import models.journeydata.isaproducts.IsaProducts
import models.journeydata.liaisonofficers.LiaisonOfficers
import models.journeydata.signatories.Signatories
import models.journeydata.thirdparty.ThirdPartyOrganisations
import uk.gov.hmrc.auth.core.{Assistant, User}

class TaskListProgressSpec extends SpecBase {

  "TaskListProgress.canAccessTaskList" - {

    "must be true only when business registration and verification have passed" in {
      TaskListProgress.canAccessTaskList(emptyJourneyDataWithBusinessVerification) mustBe true
      TaskListProgress.canAccessTaskList(emptyJourneyData) mustBe false
      TaskListProgress.canAccessTaskList(emptyJourneyDataWithFailedBusinessVerification) mustBe false
      TaskListProgress.canAccessTaskList(
        emptyJourneyDataWithBusinessVerification.copy(
          businessVerification = Some(testBV.copy(businessRegistrationPassed = Some(false)))
        )
      ) mustBe false
    }
  }

  "TaskListProgress.canSubmitAnswers" - {

    "must be true only for a User with access and all required tasks complete" in {
      TaskListProgress.canSubmitAnswers(completeTaskListJourneyData, User) mustBe true
      TaskListProgress.canSubmitAnswers(completeTaskListJourneyData, Assistant) mustBe false
      TaskListProgress.canSubmitAnswers(
        completeTaskListJourneyData.copy(businessVerification = None),
        User
      ) mustBe false
      TaskListProgress.canSubmitAnswers(completeTaskListJourneyData.copy(organisationEmail = None), User) mustBe false
    }
  }

  "TaskListProgress.allRequiredTasksComplete" - {

    "must be true only when every required task is complete" in {
      TaskListProgress.allRequiredTasksComplete(completeTaskListJourneyData) mustBe true
      TaskListProgress.allRequiredTasksComplete(completeTaskListJourneyData.copy(signatories = None)) mustBe false
    }
  }

  "TaskListProgress.isOrganisationInformationComplete" - {

    "must support completed organisation information paths" in {
      TaskListProgress.isOrganisationInformationComplete(
        emptyJourneyData.copy(organisationDetails = Some(completeTaskListOrganisationDetails))
      ) mustBe true

      TaskListProgress.isOrganisationInformationComplete(
        emptyJourneyData.copy(organisationDetails =
          Some(
            completeTaskListOrganisationDetails.copy(
              registeredToManageIsa = Some(YesNoAnswer.Yes),
              zRefNumber = Some("Z1234")
            )
          )
        )
      ) mustBe true

      TaskListProgress.isOrganisationInformationComplete(
        emptyJourneyData.copy(organisationDetails =
          Some(
            completeTaskListOrganisationDetails.copy(
              registeredAddressCorrespondence = Some(YesNoAnswer.No),
              correspondenceAddress = Some(testCorrespondenceAddress)
            )
          )
        )
      ) mustBe true
    }

    "must be false when required organisation information is missing" in {
      Seq(
        completeTaskListOrganisationDetails.copy(registeredToManageIsa = None),
        completeTaskListOrganisationDetails.copy(
          registeredToManageIsa = Some(YesNoAnswer.Yes),
          zRefNumber = None
        ),
        completeTaskListOrganisationDetails.copy(
          tradingUsingDifferentName = Some(YesNoAnswer.Yes),
          tradingName = None
        ),
        completeTaskListOrganisationDetails.copy(fcaNumber = None),
        completeTaskListOrganisationDetails.copy(
          registeredAddressCorrespondence = Some(YesNoAnswer.No),
          correspondenceAddress = None
        ),
        completeTaskListOrganisationDetails.copy(orgTelephoneNumber = None)
      ).foreach { organisationDetails =>
        TaskListProgress.isOrganisationInformationComplete(
          emptyJourneyData.copy(organisationDetails = Some(organisationDetails))
        ) mustBe false
      }
    }
  }

  "TaskListProgress.isOrganisationEmailVerified" - {

    "must be true only when an email is present and verified" in {
      TaskListProgress.isOrganisationEmailVerified(
        emptyJourneyData.copy(organisationEmail = Some(completeTaskListOrganisationEmail))
      ) mustBe true

      Seq(
        OrganisationEmail(None, Some(true)),
        OrganisationEmail(Some(""), Some(true)),
        unverifiedTaskListOrganisationEmail,
        OrganisationEmail(Some("test@example.com"), None)
      ).foreach { organisationEmail =>
        TaskListProgress.isOrganisationEmailVerified(
          emptyJourneyData.copy(organisationEmail = Some(organisationEmail))
        ) mustBe false
      }
    }
  }

  "TaskListProgress.isIsaProductsComplete" - {

    "must be true for complete ISA product answers" in {
      Seq(
        completeTaskListIsaProducts,
        IsaProducts(
          isaProducts = Some(Seq(InnovativeFinanceIsas)),
          innovativeFinancialProducts = Some(Seq(PeertopeerLoansUsingAPlatformWith36hPermissions)),
          p2pPlatform = Some("platform"),
          p2pPlatformNumber = Some("1234567")
        )
      ).foreach { isaProducts =>
        TaskListProgress.isIsaProductsComplete(emptyJourneyData.copy(isaProducts = Some(isaProducts))) mustBe true
      }
    }

    "must be false when required ISA product answers are missing" in {
      Seq(
        IsaProducts(),
        IsaProducts(isaProducts = Some(Nil)),
        IsaProducts(isaProducts = Some(Seq(InnovativeFinanceIsas))),
        IsaProducts(
          isaProducts = Some(Seq(InnovativeFinanceIsas)),
          innovativeFinancialProducts = Some(Seq(PeertopeerLoansUsingAPlatformWith36hPermissions)),
          p2pPlatform = Some("platform")
        )
      ).foreach { isaProducts =>
        TaskListProgress.isIsaProductsComplete(emptyJourneyData.copy(isaProducts = Some(isaProducts))) mustBe false
      }
    }
  }

  "TaskListProgress.isCertificatesOfAuthorityComplete" - {

    "must be true for complete certificates of authority answers" in {
      Seq(
        CertificatesOfAuthority(certificatesYesNo = Some(CertificatesYes), fcaArticles = Some(Seq(Article14))),
        CertificatesOfAuthority(
          certificatesYesNo = Some(CertificatesNo),
          financialOrganisation = Some(Seq(EuropeanInstitution))
        )
      ).foreach { certificatesOfAuthority =>
        TaskListProgress.isCertificatesOfAuthorityComplete(
          emptyJourneyData.copy(certificatesOfAuthority = Some(certificatesOfAuthority))
        ) mustBe true
      }
    }

    "must be false when required certificates of authority answers are missing" in {
      Seq(
        CertificatesOfAuthority(),
        CertificatesOfAuthority(certificatesYesNo = Some(CertificatesYes)),
        CertificatesOfAuthority(certificatesYesNo = Some(CertificatesNo))
      ).foreach { certificatesOfAuthority =>
        TaskListProgress.isCertificatesOfAuthorityComplete(
          emptyJourneyData.copy(certificatesOfAuthority = Some(certificatesOfAuthority))
        ) mustBe false
      }
    }
  }

  "TaskListProgress.areLiaisonOfficersComplete" - {

    "must be true only when at least one liaison officer is present and all are complete" in {
      TaskListProgress.areLiaisonOfficersComplete(
        emptyJourneyData.copy(liaisonOfficers = Some(liaisonOfficersWith(completeTaskListLiaisonOfficer("lo-1"))))
      ) mustBe true

      TaskListProgress.areLiaisonOfficersComplete(
        emptyJourneyData.copy(liaisonOfficers = Some(LiaisonOfficers()))
      ) mustBe false

      TaskListProgress.areLiaisonOfficersComplete(
        emptyJourneyData.copy(liaisonOfficers = Some(liaisonOfficersWith(inProgressTaskListLiaisonOfficer("lo-1"))))
      ) mustBe false
    }
  }

  "TaskListProgress.areSignatoriesComplete" - {

    "must be true only when at least one signatory is present and all are complete" in {
      TaskListProgress.areSignatoriesComplete(
        emptyJourneyData.copy(signatories = Some(signatoriesWith(completeTaskListSignatory("sig-1"))))
      ) mustBe true

      TaskListProgress.areSignatoriesComplete(emptyJourneyData.copy(signatories = Some(Signatories()))) mustBe false

      TaskListProgress.areSignatoriesComplete(
        emptyJourneyData.copy(signatories = Some(signatoriesWith(inProgressTaskListSignatory("sig-1"))))
      ) mustBe false
    }
  }

  "TaskListProgress.areThirdPartyOrganisationsComplete" - {

    "must be true when third parties are not used or all required third parties are complete" in {
      TaskListProgress.areThirdPartyOrganisationsComplete(
        emptyJourneyData.copy(thirdPartyOrganisations = Some(thirdPartyOrganisationsNotUsed))
      ) mustBe true

      TaskListProgress.areThirdPartyOrganisationsComplete(
        emptyJourneyData.copy(thirdPartyOrganisations =
          Some(testThirdPartyOrganisations(Seq(completeTaskListThirdParty("tp-1"))))
        )
      ) mustBe true
    }

    "must be false when third parties are used but missing or incomplete" in {
      Seq(
        ThirdPartyOrganisations(managedByThirdParty = Some(YesNoAnswer.Yes)),
        testThirdPartyOrganisations(Seq(inProgressTaskListThirdParty("tp-1")))
      ).foreach { thirdPartyOrganisations =>
        TaskListProgress.areThirdPartyOrganisationsComplete(
          emptyJourneyData.copy(thirdPartyOrganisations = Some(thirdPartyOrganisations))
        ) mustBe false
      }
    }
  }
}
