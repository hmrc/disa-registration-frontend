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

package viewmodels.govuk.checkAnswers.submission

import base.SpecBase
import controllers.certificatesofauthority.routes.CertificatesOfAuthorityYesNoController
import controllers.isaproducts.routes.{IsaProductsController, PeerToPeerPlatformNumberController}
import controllers.liaisonofficers.routes.{AddedLiaisonOfficersController, LiaisonOfficerNameController}
import controllers.orgdetails.routes.RegisteredIsaManagerController
import controllers.orgemail.routes.OrganisationEmailAddressController
import controllers.signatories.routes.{AddedSignatoryController, SignatoryNameController}
import controllers.thirdparty.routes.{ProductsManagedByThirdPartyController, ThirdPartyOrgDetailsController}
import models.ReturnTo.SubmissionCya
import models.journeydata.certificatesofauthority.CertificatesOfAuthority
import models.journeydata.certificatesofauthority.CertificatesOfAuthorityYesNo.Yes
import models.journeydata.isaproducts.InnovativeFinancialProduct.PeertopeerLoansUsingAPlatformWith36hPermissions
import models.journeydata.isaproducts.IsaProduct.{CashIsas, InnovativeFinanceIsas}
import models.journeydata.isaproducts.IsaProducts
import models.journeydata.liaisonofficers.LiaisonOfficerCommunication.ByEmail
import models.journeydata.liaisonofficers.{LiaisonOfficer, LiaisonOfficers}
import models.journeydata.signatories.{Signatories, Signatory}
import models.journeydata.thirdparty.{ThirdParty, ThirdPartyOrganisations}
import models.journeydata.{JourneyData, OrganisationDetails, OrganisationEmail}
import models.{CheckMode, YesNoAnswer}
import viewmodels.checkAnswers.submission.SubmissionCyaViewModel

class SubmissionCyaViewModelSpec extends SpecBase {

  "SubmissionCyaViewModel" - {

    "must not render empty sections" in {
      val viewModel = SubmissionCyaViewModel(emptyJourneyData)

      viewModel.sections mustBe empty
    }

    "must build populated sections with Submission CYA return links" in {
      val viewModel = SubmissionCyaViewModel(populatedJourneyData)
      val hrefs     = actionHrefs(viewModel)

      viewModel.sections.map(_.heading)    must contain allOf (
        messages("submissionCya.organisationInformation.heading"),
        messages("submissionCya.organisationEmail.heading"),
        messages("submissionCya.productsAndCertificates.heading"),
        messages("submissionCya.liaisonOfficer.heading"),
        messages("submissionCya.signatory.heading"),
        messages("submissionCya.thirdPartyOrganisations.heading")
      )

      hrefs                                must contain allOf (
        RegisteredIsaManagerController.onPageLoad(CheckMode, Some(SubmissionCya)).url,
        OrganisationEmailAddressController.onPageLoad(CheckMode, Some(SubmissionCya)).url,
        IsaProductsController.onPageLoad(CheckMode, Some(SubmissionCya)).url,
        PeerToPeerPlatformNumberController.onPageLoad(CheckMode, Some(SubmissionCya)).url,
        CertificatesOfAuthorityYesNoController.onPageLoad(CheckMode, Some(SubmissionCya)).url,
        LiaisonOfficerNameController.onPageLoad(Some(liaisonOfficerId), CheckMode, Some(SubmissionCya)).url,
        AddedLiaisonOfficersController.onPageLoad(CheckMode, Some(SubmissionCya)).url,
        SignatoryNameController.onPageLoad(Some(signatoryId), CheckMode, Some(SubmissionCya)).url,
        AddedSignatoryController.onPageLoad(CheckMode, Some(SubmissionCya)).url,
        ProductsManagedByThirdPartyController.onPageLoad(CheckMode, Some(SubmissionCya)).url,
        ThirdPartyOrgDetailsController.onPageLoad(Some(thirdPartyId), CheckMode, Some(SubmissionCya)).url
      )

      hrefs.filter(_.contains("returnTo")) must not be empty
      hrefs.filter(_.contains("returnTo")).foreach(_ must include("returnTo=SubmissionCya"))
    }
  }

  private val liaisonOfficerId = "liaison-officer-1"
  private val signatoryId      = "signatory-1"
  private val thirdPartyId     = "third-party-1"

  private def actionHrefs(viewModel: SubmissionCyaViewModel): Seq[String] =
    viewModel.sections
      .flatMap(_.rows)
      .flatMap(_.actions.toSeq)
      .flatMap(_.items.map(_.href))

  private def populatedJourneyData: JourneyData =
    emptyJourneyData.copy(
      businessVerification = Some(
        testBV.copy(companyNumber = Some("12345678"))
      ),
      organisationDetails = Some(
        OrganisationDetails(
          registeredToManageIsa = Some(YesNoAnswer.Yes),
          zRefNumber = Some("Z1234"),
          tradingUsingDifferentName = Some(YesNoAnswer.Yes),
          tradingName = Some("Trading name"),
          fcaNumber = Some("123456"),
          registeredAddressCorrespondence = Some(YesNoAnswer.Yes),
          correspondenceAddress = Some(testCorrespondenceAddress),
          orgTelephoneNumber = Some("01234567890")
        )
      ),
      organisationEmail = Some(OrganisationEmail(Some("test@example.com"), Some(true))),
      isaProducts = Some(
        IsaProducts(
          isaProducts = Some(Seq(CashIsas, InnovativeFinanceIsas)),
          p2pPlatform = Some("Platform name"),
          p2pPlatformNumber = Some("123456"),
          innovativeFinancialProducts = Some(Seq(PeertopeerLoansUsingAPlatformWith36hPermissions))
        )
      ),
      certificatesOfAuthority = Some(CertificatesOfAuthority(certificatesYesNo = Some(Yes))),
      liaisonOfficers = Some(
        LiaisonOfficers(
          Seq(
            LiaisonOfficer(
              id = liaisonOfficerId,
              fullName = Some("Liaison Officer"),
              phoneNumber = Some("01234567890"),
              communication = Set(ByEmail),
              email = Some("liaison@example.com")
            )
          )
        )
      ),
      signatories = Some(
        Signatories(
          Seq(
            Signatory(
              id = signatoryId,
              fullName = Some("Signatory"),
              jobTitle = Some("Director")
            )
          )
        )
      ),
      thirdPartyOrganisations = Some(
        ThirdPartyOrganisations(
          managedByThirdParty = Some(YesNoAnswer.Yes),
          thirdParties = Seq(
            ThirdParty(
              id = thirdPartyId,
              thirdPartyName = Some("Third Party Org"),
              thirdPartyFrn = Some("654321"),
              managingIsaReturns = Some(YesNoAnswer.Yes),
              usingInvestorFunds = Some(YesNoAnswer.Yes),
              investorFundsPercentage = Some("50")
            )
          )
        )
      )
    )
}
