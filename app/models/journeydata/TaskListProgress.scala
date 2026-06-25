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

import models.YesNoAnswer
import models.journeydata.certificatesofauthority.CertificatesOfAuthorityYesNo.{No as CertificatesNo, Yes as CertificatesYes}
import models.journeydata.isaproducts.InnovativeFinancialProduct.PeertopeerLoansUsingAPlatformWith36hPermissions
import models.journeydata.isaproducts.IsaProduct.InnovativeFinanceIsas
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
    isOrganisationInformationComplete(journeyData) &&
      isOrganisationEmailVerified(journeyData) &&
      isIsaProductsComplete(journeyData) &&
      isCertificatesOfAuthorityComplete(journeyData) &&
      areLiaisonOfficersComplete(journeyData) &&
      areSignatoriesComplete(journeyData) &&
      areThirdPartyOrganisationsComplete(journeyData)

  def isOrganisationInformationComplete(journeyData: JourneyData): Boolean =
    journeyData.organisationDetails.exists { organisationDetails =>
      organisationDetails.registeredToManageIsa.exists { registeredToManageIsa =>
        val zReferenceComplete =
          registeredToManageIsa == YesNoAnswer.No || organisationDetails.zRefNumber.exists(nonEmpty)

        val tradingNameComplete =
          organisationDetails.tradingUsingDifferentName.exists {
            case YesNoAnswer.Yes => organisationDetails.tradingName.exists(nonEmpty)
            case YesNoAnswer.No  => true
          }

        val correspondenceAddressComplete =
          organisationDetails.registeredAddressCorrespondence.exists {
            case YesNoAnswer.Yes => true
            case YesNoAnswer.No  => organisationDetails.correspondenceAddress.exists(_.isPopulated)
          }

        zReferenceComplete &&
        tradingNameComplete &&
        organisationDetails.fcaNumber.exists(nonEmpty) &&
        correspondenceAddressComplete &&
        organisationDetails.orgTelephoneNumber.exists(nonEmpty)
      }
    }

  def isOrganisationEmailVerified(journeyData: JourneyData): Boolean =
    journeyData.organisationEmail.exists { organisationEmail =>
      organisationEmail.organisationEmail.exists(nonEmpty) && organisationEmail.verified.contains(true)
    }

  def isIsaProductsComplete(journeyData: JourneyData): Boolean =
    journeyData.isaProducts.exists { isaProducts =>
      isaProducts.isaProducts.exists(_.nonEmpty) &&
      (!isaProducts.isaProducts.exists(_.contains(InnovativeFinanceIsas)) ||
        (
          isaProducts.innovativeFinancialProducts.exists(_.nonEmpty) &&
            (!isaProducts.innovativeFinancialProducts.exists(
              _.contains(PeertopeerLoansUsingAPlatformWith36hPermissions)
            ) ||
              (isaProducts.p2pPlatform.exists(nonEmpty) && isaProducts.p2pPlatformNumber.exists(nonEmpty)))
        ))
    }

  def isCertificatesOfAuthorityComplete(journeyData: JourneyData): Boolean =
    journeyData.certificatesOfAuthority.exists { certificatesOfAuthority =>
      certificatesOfAuthority.certificatesYesNo.exists {
        case CertificatesYes => certificatesOfAuthority.fcaArticles.exists(_.nonEmpty)
        case CertificatesNo  => certificatesOfAuthority.financialOrganisation.exists(_.nonEmpty)
      }
    }

  def areLiaisonOfficersComplete(journeyData: JourneyData): Boolean =
    journeyData.liaisonOfficers.exists { liaisonOfficers =>
      liaisonOfficers.liaisonOfficers.nonEmpty && liaisonOfficers.liaisonOfficers.forall(!_.inProgress)
    }

  def areSignatoriesComplete(journeyData: JourneyData): Boolean =
    journeyData.signatories.exists { signatories =>
      signatories.signatories.nonEmpty && signatories.signatories.forall(!_.inProgress)
    }

  def areThirdPartyOrganisationsComplete(journeyData: JourneyData): Boolean =
    journeyData.thirdPartyOrganisations.exists { thirdPartyOrganisations =>
      thirdPartyOrganisations.managedByThirdParty.exists {
        case YesNoAnswer.No  => true
        case YesNoAnswer.Yes =>
          thirdPartyOrganisations.thirdParties.nonEmpty && thirdPartyOrganisations.thirdParties.forall(!_.inProgress)
      }
    }

  private def nonEmpty(value: String): Boolean =
    value.trim.nonEmpty
}
