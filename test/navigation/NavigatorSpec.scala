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
import controllers.orgdetails.routes.*
import controllers.routes.IndexController
import models.*
import models.journeydata.OrganisationDetails
import models.journeydata.certificatesofauthority.CertificatesOfAuthority
import models.journeydata.certificatesofauthority.CertificatesOfAuthorityYesNo.{No, Yes}
import models.journeydata.isaproducts.InnovativeFinancialProduct.{CrowdFundedDebentures, PeertopeerLoansUsingAPlatformWith36hPermissions}
import models.journeydata.isaproducts.IsaProduct.{CashIsas, InnovativeFinanceIsas}
import models.journeydata.isaproducts.{InnovativeFinancialProduct, IsaProduct, IsaProducts}
import models.journeydata.signatories.{Signatories, Signatory}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{spy, verify, when}
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import pages.*
import pages.certificatesofauthority.{CertificatesOfAuthorityYesNoPage, FcaArticlesPage, FinancialOrganisationPage}
import pages.isaproducts.{InnovativeFinancialProductsPage, IsaProductsPage, PeerToPeerPlatformNumberPage, PeerToPeerPlatformPage}
import pages.organisationdetails.{RegisteredAddressCorrespondencePage, ZReferenceNumberPage}
import pages.signatories.{RemoveSignatoryPage, SignatoryJobTitlePage, SignatoryNamePage}
import play.api.mvc.Call

class NavigatorSpec extends SpecBase {

  private val navigator = new Navigator()

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

    "route ZReferenceNumberPage to TradingUsingDIfferentNamePage" in {
      val result: Call = navigator.normalRoutes(ZReferenceNumberPage, OrganisationDetails())

      result shouldBe TradingUsingDifferentNameController.onPageLoad(NormalMode)
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

    "route RemoveSignatoryPage to Index Controller" in {
      val result: Call = navigator.normalRoutes(RemoveSignatoryPage, signatoriesAnswers)
      result shouldBe IndexController.onPageLoad()
    }

    "route SignatoryNamePage to Index Controller" in {
      val result: Call = navigator.normalRoutes(SignatoryNamePage, signatoriesAnswers)
      result shouldBe IndexController.onPageLoad()
    }

    "route SignatoryJobTitlePage to Index Controller" in {
      val result: Call = navigator.normalRoutes(SignatoryJobTitlePage, signatoriesAnswers)
      result shouldBe IndexController.onPageLoad()
    }

    "route unknown page to Index" in {
      case object UnknownPage extends PageWithoutDependents[IsaProducts] {
        override def clearAnswer(answers: IsaProducts): IsaProducts = answers
      }

      val result: Call = navigator.normalRoutes(UnknownPage, emptyIsaProductsAnswers)

      result shouldBe IndexController.onPageLoad()
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

    "route RemoveSignatoryPage to Index Controller" in {
      navigator.checkRouteMap(RemoveSignatoryPage) shouldBe
        IndexController.onPageLoad()
    }

    "route SignatoryNamePage to Index Controller" in {
      navigator.checkRouteMap(SignatoryNamePage) shouldBe
        IndexController.onPageLoad()
    }

    "route SignatoryJobTitlePage to Index Controller" in {
      navigator.checkRouteMap(SignatoryJobTitlePage) shouldBe
        IndexController.onPageLoad()
    }

    "route unknown page to Index" in {
      case object UnknownPage extends PageWithoutDependents[IsaProducts] {
        override def clearAnswer(sectionAnswers: IsaProducts): IsaProducts = sectionAnswers
      }

      val result: Call = navigator.checkRouteMap(UnknownPage)

      result shouldBe IndexController.onPageLoad()
    }
  }
}
