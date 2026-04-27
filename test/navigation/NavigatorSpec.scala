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

package navigation

import base.SpecBase
import controllers.certificatesofauthority.routes.*
import controllers.isaproducts.routes.*
import controllers.liaisonofficers.routes.*
import controllers.orgdetails.routes.*
import controllers.routes.{IndexController, TaskListController}
import controllers.signatories.routes.*
import controllers.thirdparty.routes.*
import models.*
import models.journeydata.OrganisationDetails
import models.journeydata.certificatesofauthority.CertificatesOfAuthority
import models.journeydata.certificatesofauthority.CertificatesOfAuthorityYesNo.{No, Yes}
import models.journeydata.isaproducts.InnovativeFinancialProduct.{CrowdFundedDebentures, PeertopeerLoansUsingAPlatformWith36hPermissions}
import models.journeydata.isaproducts.IsaProduct.{CashIsas, InnovativeFinanceIsas}
import models.journeydata.isaproducts.{InnovativeFinancialProduct, IsaProduct, IsaProducts}
import models.journeydata.liaisonofficers.{LiaisonOfficer, LiaisonOfficers}
import models.journeydata.signatories.{Signatories, Signatory}
import models.journeydata.thirdparty.{ThirdParty, ThirdPartyOrganisations}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{spy, verify, when}
import org.scalatest.matchers.should.Matchers.shouldBe
import pages.*
import pages.certificatesofauthority.{CertificatesOfAuthorityYesNoPage, FcaArticlesPage, FinancialOrganisationPage}
import pages.isaproducts.{InnovativeFinancialProductsPage, IsaProductsPage, PeerToPeerPlatformNumberPage, PeerToPeerPlatformPage}
import pages.liaisonofficers.*
import pages.organisationdetails.*
import pages.signatories.*
import pages.thirdparty.{InvestorFundsUsedByThirdPartyPage, ProductsManagedByThirdPartyPage, ReturnsManagedByThirdPartyPage, ThirdPartyOrgDetailsPage}
import play.api.mvc.Call

class NavigatorSpec extends SpecBase {

  private val navigator   = new Navigator()
  private val signatoryId = "testId"

  private def answersWithIsaProducts(products: Seq[IsaProduct]): IsaProducts =
    IsaProducts(
      isaProducts = Some(products),
      innovativeFinancialProducts = None
    )

  private def answersWithInnovativeFinancialProducts(ifps: Seq[InnovativeFinancialProduct]): IsaProducts =
    IsaProducts(
      isaProducts = Some(Seq(InnovativeFinanceIsas)),
      innovativeFinancialProducts = Some(ifps)
    )

  private val emptyIsaProductsAnswers: IsaProducts =
    IsaProducts(
      isaProducts = None,
      innovativeFinancialProducts = None
    )

  private val coaAnswers: CertificatesOfAuthority =
    CertificatesOfAuthority(certificatesYesNo = Some(Yes), fcaArticles = None, financialOrganisation = None)

  private val signatoriesAnswers: Signatories =
    Signatories(Seq(Signatory(id = testString, fullName = Some(testString), jobTitle = Some(testString))))

  private val liaisonOfficersAnswers: LiaisonOfficers =
    LiaisonOfficers(Seq(LiaisonOfficer(id = testString, fullName = Some(testString))))

  "Navigator.nextPage(PageWithDependents)" - {

    "resume to NormalMode when in CheckMode and resumeNormalMode is true" in {
      val pageMock              = mock[PageWithDependents[IsaProducts]]
      val answerWithIfpSelected = answersWithIsaProducts(Seq(InnovativeFinanceIsas))

      when(pageMock.resumeNormalMode(any)).thenReturn(true)

      val result: Call =
        navigator.nextPage(
          page = IsaProductsPage,
          existing = Some(emptyIsaProductsAnswers),
          updated = answerWithIfpSelected,
          mode = CheckMode
        )

      result shouldBe InnovativeFinancialProductsController.onPageLoad(NormalMode)
    }

    "stay in CheckMode when resumeNormalMode is false" in {
      val pageMock = mock[PageWithDependents[IsaProducts]]

      when(pageMock.resumeNormalMode(any)).thenReturn(false)

      val result =
        navigator.nextPage(
          page = InnovativeFinancialProductsPage,
          existing = Some(emptyIsaProductsAnswers),
          updated = emptyIsaProductsAnswers,
          mode = CheckMode
        )

      result shouldBe IsaProductsCheckYourAnswersController.onPageLoad()
    }

    "default to NormalMode when no existing data is present" in {
      val result =
        navigator.nextPage(
          page = IsaProductsPage,
          existing = None,
          updated = answersWithIsaProducts(Seq(InnovativeFinanceIsas)),
          mode = CheckMode
        )

      result shouldBe InnovativeFinancialProductsController.onPageLoad(NormalMode)
    }
  }

  "Navigator.nextPage(PageWithoutDependents)" - {

    "look up normal routes in Normal Mode" in {
      val spiedNav = spy(new Navigator())

      spiedNav.nextPage(
        page = PeerToPeerPlatformPage,
        updated = emptyIsaProductsAnswers,
        mode = NormalMode
      )

      verify(spiedNav).normalRoutes(any, any)
    }

    "lookup check routes in Check Mode" in {
      val spiedNav = spy(new Navigator())

      spiedNav.nextPage(
        page = PeerToPeerPlatformPage,
        updated = emptyIsaProductsAnswers,
        mode = CheckMode
      )

      verify(spiedNav).checkRouteMap(any)
    }
  }

  "Navigator.normalRoutes" - {

    "route TradingUsingDifferentNamePage to TradingNamePage when Yes selected" in {
      val answers = OrganisationDetails(tradingUsingDifferentName = Some(true))

      val result: Call = navigator.normalRoutes(TradingUsingDifferentNamePage, answers)

      result shouldBe TradingNameController.onPageLoad(NormalMode)
    }

    "route TradingUsingDifferentNamePage to FirmReferenceNumberPage when No selected" in {
      val answers = OrganisationDetails(tradingUsingDifferentName = Some(false))

      val result: Call = navigator.normalRoutes(TradingUsingDifferentNamePage, answers)

      result shouldBe FirmReferenceNumberController.onPageLoad(NormalMode)
    }

    "route TradingNamePage to FirmReferenceNumberPage" in {
      val result: Call = navigator.normalRoutes(TradingNamePage, OrganisationDetails())
      result shouldBe FirmReferenceNumberController.onPageLoad(NormalMode)
    }

    "route IsaProductsPage  to IF products when IF ISA selected" in {
      val answers = answersWithIsaProducts(Seq(InnovativeFinanceIsas))

      val result: Call = navigator.normalRoutes(IsaProductsPage, answers)

      result shouldBe InnovativeFinancialProductsController.onPageLoad(NormalMode)
    }

    "route IsaProductsPage to CYA when IF ISA not selected" in {
      val answers = answersWithIsaProducts(Seq(CashIsas))

      val result: Call = navigator.normalRoutes(IsaProductsPage, answers)

      result shouldBe IsaProductsCheckYourAnswersController.onPageLoad()
    }

    "route IsaProductsPage to Index when isaProducts is missing" in {
      val answers = emptyIsaProductsAnswers.copy(isaProducts = None)

      val result: Call = navigator.normalRoutes(IsaProductsPage, answers)

      result shouldBe IndexController.onPageLoad()
    }

    "route InnovativeFinancialProductsPage to P2P platform when 36H selected" in {
      val answers = answersWithInnovativeFinancialProducts(Seq(PeertopeerLoansUsingAPlatformWith36hPermissions))

      val result: Call = navigator.normalRoutes(InnovativeFinancialProductsPage, answers)

      result shouldBe PeerToPeerPlatformController.onPageLoad(NormalMode)
    }

    "route InnovativeFinancialProductsPage to CYA when 36H not selected" in {
      val answers = answersWithInnovativeFinancialProducts(Seq(CrowdFundedDebentures))

      val result: Call = navigator.normalRoutes(InnovativeFinancialProductsPage, answers)

      result shouldBe IsaProductsCheckYourAnswersController.onPageLoad()
    }

    "route InnovativeFinancialProductsPage to Index when innovativeFinancialProducts is missing" in {
      val answers = emptyIsaProductsAnswers.copy(innovativeFinancialProducts = None)

      val result: Call = navigator.normalRoutes(InnovativeFinancialProductsPage, answers)

      result shouldBe IndexController.onPageLoad()
    }

    "route PeerToPeerPlatformPage to PeerToPeerPlatformNumberPage" in {
      val result: Call = navigator.normalRoutes(PeerToPeerPlatformPage, emptyIsaProductsAnswers)

      result shouldBe PeerToPeerPlatformNumberController.onPageLoad(NormalMode)
    }

    "route PeerToPeerPlatformNumberPage to ISA products CYA" in {
      val result: Call = navigator.normalRoutes(PeerToPeerPlatformNumberPage, emptyIsaProductsAnswers)

      result shouldBe IsaProductsCheckYourAnswersController.onPageLoad()
    }

    "route CertificatesOfAuthorityYesNoPage to FcaArticlesPage if yes submitted" in {
      val result: Call = navigator.normalRoutes(CertificatesOfAuthorityYesNoPage, coaAnswers)
      result shouldBe FcaArticlesController.onPageLoad(NormalMode)
    }

    "route CertificatesOfAuthorityYesNoPage to FinancialOrganisationPage if no submitted" in {
      val result: Call =
        navigator.normalRoutes(CertificatesOfAuthorityYesNoPage, coaAnswers.copy(certificatesYesNo = Some(No)))
      result shouldBe FinancialOrganisationController.onPageLoad(NormalMode)
    }

    "route to CertificatesOfAuthorityYesNoPage if no answer is present for certificatesYesNo" in {
      val result: Call =
        navigator.normalRoutes(CertificatesOfAuthorityYesNoPage, coaAnswers.copy(certificatesYesNo = None))
      result shouldBe CertificatesOfAuthorityYesNoController.onPageLoad(NormalMode)
    }

    "route RegisteredAddressCorrespondencePage to RegisteredAddressCorrespondenceController" in {
      val result: Call = navigator.normalRoutes(FirmReferenceNumberPage, testOrganisationDetails)
      result shouldBe RegisteredAddressCorrespondenceController.onPageLoad(NormalMode)
    }

    "route RegisteredAddressCorrespondencePage to OrganisationTelephoneNumberController if yes submitted" in {
      val result: Call = navigator.normalRoutes(
        RegisteredAddressCorrespondencePage,
        testOrganisationDetails.copy(registeredAddressCorrespondence = Some(true))
      )
      result shouldBe OrganisationTelephoneNumberController.onPageLoad(NormalMode)
    }

    "route RegisteredAddressCorrespondencePage to Address Lookup page if no submitted" in {
      val result: Call =
        navigator.normalRoutes(
          RegisteredAddressCorrespondencePage,
          testOrganisationDetails.copy(registeredAddressCorrespondence = Some(false))
        )
      result shouldBe IndexController.onPageLoad()
    }

    "route to RegisteredAddressCorrespondencePage if no answer is present for registeredAddressCorrespondence" in {
      val result: Call =
        navigator.normalRoutes(
          RegisteredAddressCorrespondencePage,
          testOrganisationDetails.copy(registeredAddressCorrespondence = None)
        )
      result shouldBe IndexController.onPageLoad()
    }

    "route FcaArticlesPage to ISA products CYA" in {
      val result: Call = navigator.normalRoutes(FcaArticlesPage, coaAnswers)
      result shouldBe CoaCheckYourAnswersController.onPageLoad()
    }

    "route FinancialOrganisationPage to ISA products CYA" in {
      val result: Call = navigator.normalRoutes(FinancialOrganisationPage, coaAnswers)
      result shouldBe CoaCheckYourAnswersController.onPageLoad()
    }

    "route RemoveSignatoryPage to AddedSignatoryController when signatory exists in journeyAnswers" in {
      val result: Call = navigator.normalRoutes(RemoveSignatoryPage(signatoryId), signatoriesAnswers)
      result shouldBe AddedSignatoryController.onPageLoad()
    }

    "route RemoveSignatoryPage to AddedSignatoryController when a signatory doesn't exists in journeyAnswers" in {
      val result: Call = navigator.normalRoutes(RemoveSignatoryPage(signatoryId), Signatories(Seq.empty))
      result shouldBe AddASignatoryController.onPageLoad()
    }

    "route SignatoryNamePage to SignatoryJobTitleController" in {
      val result: Call = navigator.normalRoutes(SignatoryNamePage(signatoryId), signatoriesAnswers)
      result shouldBe SignatoryJobTitleController.onPageLoad(id = signatoryId, mode = NormalMode)
    }

    "route SignatoryJobTitlePage to SignatoryCheckYourAnswersController" in {
      val result: Call = navigator.normalRoutes(SignatoryJobTitlePage(signatoryId), signatoriesAnswers)
      result shouldBe SignatoryCheckYourAnswersController.onPageLoad(id = signatoryId)
    }

    "route LiaisonOfficerNamePage to LiaisonOfficerEmailController" in {
      val result: Call = navigator.normalRoutes(LiaisonOfficerNamePage(testString), liaisonOfficersAnswers)

      result shouldBe LiaisonOfficerEmailController.onPageLoad(testString, NormalMode)
    }

    "route LiaisonOfficerEmailPage to LiaisonOfficerPhoneNumberController" in {
      val result: Call = navigator.normalRoutes(LiaisonOfficerEmailPage(testString), liaisonOfficersAnswers)

      result shouldBe LiaisonOfficerPhoneNumberController.onPageLoad(testString, NormalMode)
    }

    "route LiaisonOfficerPhoneNumberPage to LiaisonOfficerCommunicationController" in {
      val result: Call = navigator.normalRoutes(LiaisonOfficerPhoneNumberPage(testString), liaisonOfficersAnswers)

      result shouldBe LiaisonOfficerCommunicationController.onPageLoad(testString, NormalMode)
    }

    "route LiaisonOfficerCommunicationPage to LO CYA" in {
      val result: Call = navigator.normalRoutes(LiaisonOfficerCommunicationPage(testString), liaisonOfficersAnswers)

      result shouldBe LoCheckYourAnswersController.onPageLoad(testString)
    }

    "route RemoveLiaisonOfficerPage to AddLiaisonOfficerController when no liaison officers remain" in {
      val result: Call = navigator.normalRoutes(RemoveLiaisonOfficerPage, LiaisonOfficers(Nil))

      result shouldBe AddLiaisonOfficerController.onPageLoad()
    }

    "route RemoveLiaisonOfficerPage to AddedLiaisonOfficersController when liaison officers remain" in {
      val result: Call = navigator.normalRoutes(RemoveLiaisonOfficerPage, liaisonOfficersAnswers)

      result shouldBe AddedLiaisonOfficersController.onPageLoad()
    }

    "route ProductsManagedByThirdParty to TaskList when answer is no" in {
      val result: Call =
        navigator.normalRoutes(ProductsManagedByThirdPartyPage, ThirdPartyOrganisations(Some(YesNoAnswer.No)))

      result shouldBe TaskListController.onPageLoad()
    }

    "route ProductsManagedByThirdParty to ThirdPartyOrgDetailsPage when answer is yes" in {
      val result: Call =
        navigator.normalRoutes(ProductsManagedByThirdPartyPage, ThirdPartyOrganisations(Some(YesNoAnswer.Yes)))

      result shouldBe ThirdPartyOrgDetailsController.onPageLoad(id = None, mode = NormalMode)
    }

    "route ThirdPartyOrgDetailsPage to ReturnsManagedByThirdParty" in {
      val result: Call = navigator.normalRoutes(
        ThirdPartyOrgDetailsPage(testString),
        ThirdPartyOrganisations(Some(YesNoAnswer.No), Seq(ThirdParty(testString)))
      )

      result shouldBe ReturnsManagedByThirdPartyController.onPageLoad(testString, NormalMode)
    }

    "route ReturnsManagedByThirdPartyPage to InvestorFundsUsedByThirdPartyController when 'yes' is submitted" in {
      val result: Call = navigator.normalRoutes(
        ReturnsManagedByThirdPartyPage(testString),
        ThirdPartyOrganisations(
          Some(YesNoAnswer.No),
          Seq(ThirdParty(testString, Some(testString), None, Some(YesNoAnswer.Yes)))
        )
      )

      result shouldBe InvestorFundsUsedByThirdPartyController.onPageLoad(testString, NormalMode)
    }

    "route ReturnsManagedByThirdPartyPage to InvestorFundsUsedByThirdPartyController when 'no' is submitted" in {
      val result: Call = navigator.normalRoutes(
        ReturnsManagedByThirdPartyPage(testString),
        ThirdPartyOrganisations(
          Some(YesNoAnswer.No),
          Seq(ThirdParty(testString, Some(testString), None, Some(YesNoAnswer.No)))
        )
      )

      result shouldBe InvestorFundsUsedByThirdPartyController.onPageLoad(id = testString, mode = NormalMode)
    }

    "route InvestorFundsUsedByThirdPartyPage to TaskList when 'yes' is submitted" in {
      val result: Call = navigator.normalRoutes(
        InvestorFundsUsedByThirdPartyPage(testString),
        ThirdPartyOrganisations(
          Some(YesNoAnswer.No),
          Seq(ThirdParty(testString, Some(testString), None, Some(YesNoAnswer.Yes), Some(YesNoAnswer.Yes)))
        )
      )

      result shouldBe TaskListController.onPageLoad()
    }

    "route InvestorFundsUsedByThirdPartyPage to TaskList when 'no' is submitted" in {
      val result: Call = navigator.normalRoutes(
        InvestorFundsUsedByThirdPartyPage(testString),
        ThirdPartyOrganisations(
          Some(YesNoAnswer.No),
          Seq(ThirdParty(testString, Some(testString), None, Some(YesNoAnswer.Yes), Some(YesNoAnswer.No)))
        )
      )

      result shouldBe TaskListController.onPageLoad()
    }

    "route unknown page to Index" in {
      case object UnknownPage extends PageWithoutDependents[IsaProducts] {
        override def clearAnswer(answers: IsaProducts): IsaProducts = answers
      }

      assertThrows[NotImplementedError] {
        navigator.normalRoutes(UnknownPage, emptyIsaProductsAnswers)
      }
    }
  }

  "Navigator.checkRouteMap" - {

    "route IsaProductsPage to ISA products CYA" in {
      navigator.checkRouteMap(IsaProductsPage) shouldBe IsaProductsCheckYourAnswersController.onPageLoad()
    }

    "route InnovativeFinancialProductsPage to ISA products CYA" in {
      navigator.checkRouteMap(InnovativeFinancialProductsPage) shouldBe IsaProductsCheckYourAnswersController
        .onPageLoad()
    }

    "route PeerToPeerPlatformPage to ISA products CYA" in {
      navigator.checkRouteMap(PeerToPeerPlatformPage) shouldBe IsaProductsCheckYourAnswersController.onPageLoad()
    }

    "route PeerToPeerPlatformNumberPage to ISA products CYA" in {
      navigator.checkRouteMap(PeerToPeerPlatformNumberPage) shouldBe IsaProductsCheckYourAnswersController.onPageLoad()
    }

    "route CertificatesOfAuthorityYesNoPage to COA CYA" in {
      navigator.checkRouteMap(CertificatesOfAuthorityYesNoPage) shouldBe
        CoaCheckYourAnswersController.onPageLoad()
    }

    "route FcaArticlesPage to COA CYA" in {
      navigator.checkRouteMap(FcaArticlesPage) shouldBe
        CoaCheckYourAnswersController.onPageLoad()
    }

    "route FinancialOrganisationPage to COA CYA" in {
      navigator.checkRouteMap(FinancialOrganisationPage) shouldBe
        CoaCheckYourAnswersController.onPageLoad()
    }

    "route RegisteredAddressCorrespondencePage to COA CYA" in {
      navigator.checkRouteMap(RegisteredAddressCorrespondencePage) shouldBe
        IndexController.onPageLoad()
    }

    "route SignatoryNamePage to SignatoryCheckYourAnswersController" in {
      navigator.checkRouteMap(SignatoryNamePage(signatoryId)) shouldBe
        SignatoryCheckYourAnswersController.onPageLoad(id = signatoryId)
    }

    "route SignatoryJobTitlePage to SignatoryCheckYourAnswersController" in {
      navigator.checkRouteMap(SignatoryJobTitlePage(signatoryId)) shouldBe
        SignatoryCheckYourAnswersController.onPageLoad(id = signatoryId)
    }

    "route LiaisonOfficerNamePage to LO CYA" in {
      navigator.checkRouteMap(LiaisonOfficerNamePage(testString)) shouldBe
        LoCheckYourAnswersController.onPageLoad(testString)
    }

    "route LiaisonOfficerEmailPage to LO CYA" in {
      navigator.checkRouteMap(LiaisonOfficerEmailPage(testString)) shouldBe
        LoCheckYourAnswersController.onPageLoad(testString)
    }

    "route LiaisonOfficerPhoneNumberPage to LO CYA" in {
      navigator.checkRouteMap(LiaisonOfficerPhoneNumberPage(testString)) shouldBe
        LoCheckYourAnswersController.onPageLoad(testString)
    }

    "route LiaisonOfficerCommunicationPage to LO CYA" in {
      navigator.checkRouteMap(LiaisonOfficerCommunicationPage(testString)) shouldBe
        LoCheckYourAnswersController.onPageLoad(testString)
    }

    "route unknown page to Index" in {
      case object UnknownPage extends PageWithoutDependents[IsaProducts] {
        override def clearAnswer(sectionAnswers: IsaProducts): IsaProducts = sectionAnswers
      }

      assertThrows[NotImplementedError] {
        navigator.normalRoutes(UnknownPage, emptyIsaProductsAnswers)
      }
    }
  }
}
