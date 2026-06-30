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
import controllers.orgemail.routes.*
import controllers.routes.{SubmissionCyaController, TaskListController}
import controllers.signatories.routes.*
import controllers.thirdparty.routes.*
import models.*
import models.ReturnTo.{MultipleThirdPartiesCya, MultipleThirdPartiesCyaViaAddedThirdParties, OrganisationDetailsCya, SubmissionCya, SubmissionCyaViaAddedThirdParties}
import models.addresslookup.LookupAddress
import models.journeydata.certificatesofauthority.CertificatesOfAuthority
import models.journeydata.certificatesofauthority.CertificatesOfAuthorityYesNo.{No, Yes}
import models.journeydata.isaproducts.InnovativeFinancialProduct.{CrowdFundedDebentures, PeertopeerLoansUsingAPlatformWith36hPermissions}
import models.journeydata.isaproducts.IsaProduct.{CashIsas, InnovativeFinanceIsas}
import models.journeydata.isaproducts.{InnovativeFinancialProduct, IsaProduct, IsaProducts}
import models.journeydata.liaisonofficers.{LiaisonOfficer, LiaisonOfficers}
import models.journeydata.orgdetails.SelectedCorrespondenceAddress.ManualEntry
import models.journeydata.orgdetails.{AddAnotherAddress, SelectedCorrespondenceAddress}
import models.journeydata.signatories.{Signatories, Signatory}
import models.journeydata.thirdparty.{ThirdParty, ThirdPartyOrganisations}
import models.journeydata.{CorrespondenceAddress, OrganisationDetails, OrganisationEmail}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{spy, verify, when}
import org.scalatest.matchers.should.Matchers.shouldBe
import pages.*
import pages.certificatesofauthority.{CertificatesOfAuthorityYesNoPage, FcaArticlesPage, FinancialOrganisationPage}
import pages.isaproducts.{InnovativeFinancialProductsPage, IsaProductsPage, PeerToPeerPlatformNumberPage, PeerToPeerPlatformPage}
import pages.liaisonofficers.*
import pages.organisationdetails.*
import pages.orgemail.{OrganisationEmailAddressPage, OrganisationEmailVerificationCodePage}
import pages.signatories.*
import pages.thirdparty.*
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
          mode = CheckMode,
          returnTo = None
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
          mode = CheckMode,
          returnTo = None
        )

      result shouldBe IsaProductsCheckYourAnswersController.onPageLoad()
    }

    "default to NormalMode when no existing data is present" in {
      val result =
        navigator.nextPage(
          page = IsaProductsPage,
          existing = None,
          updated = answersWithIsaProducts(Seq(InnovativeFinanceIsas)),
          mode = CheckMode,
          returnTo = None
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
        mode = NormalMode,
        returnTo = None
      )

      verify(spiedNav).normalRoutes(any, any, any)
    }

    "lookup check routes in Check Mode" in {
      val spiedNav = spy(new Navigator())

      spiedNav.nextPage(
        page = PeerToPeerPlatformPage,
        updated = emptyIsaProductsAnswers,
        mode = CheckMode,
        returnTo = None
      )

      verify(spiedNav).checkRouteMap(any, any)
    }
  }

  "Navigator.normalRoutes" - {

    "route RegisteredIsaManagerPage to ZReferenceNumberPage when yes selected and returnTo is OrganisationDetailsCya" in {
      val answers =
        OrganisationDetails(registeredToManageIsa = Some(YesNoAnswer.Yes))

      val result: Call =
        navigator.normalRoutes(
          RegisteredIsaManagerPage,
          answers,
          Some(OrganisationDetailsCya)
        )

      result shouldBe ZReferenceNumberController.onPageLoad(
        NormalMode,
        Some(OrganisationDetailsCya)
      )
    }

    "route RegisteredIsaManagerPage to OrganisationDetails CYA when no selected and returnTo is OrganisationDetailsCya" in {
      val answers =
        OrganisationDetails(registeredToManageIsa = Some(YesNoAnswer.No))

      val result: Call =
        navigator.normalRoutes(
          RegisteredIsaManagerPage,
          answers,
          Some(OrganisationDetailsCya)
        )

      result shouldBe OrganisationDetailsCheckYourAnswersController.onPageLoad()
    }

    "route RegisteredIsaManagerPage to ZReferenceNumberPage when yes selected" in {
      val answers =
        OrganisationDetails(registeredToManageIsa = Some(YesNoAnswer.Yes))

      val result: Call =
        navigator.normalRoutes(
          RegisteredIsaManagerPage,
          answers,
          None
        )

      result shouldBe ZReferenceNumberController.onPageLoad(
        NormalMode,
        None
      )
    }

    "route RegisteredIsaManagerPage to TradingUsingDifferentNamePage when no selected" in {
      val answers =
        OrganisationDetails(registeredToManageIsa = Some(YesNoAnswer.No))

      val result: Call =
        navigator.normalRoutes(
          RegisteredIsaManagerPage,
          answers,
          None
        )

      result shouldBe TradingUsingDifferentNameController.onPageLoad(
        NormalMode,
        None
      )
    }

    "route RegisteredIsaManagerPage to TaskList when no answer is present" in {
      val result: Call =
        navigator.normalRoutes(
          RegisteredIsaManagerPage,
          OrganisationDetails(),
          None
        )

      result shouldBe TaskListController.onPageLoad()
    }

    "route ZReferenceNumberPage to OrganisationDetails CYA when returnTo is OrganisationDetailsCya" in {
      val result: Call =
        navigator.normalRoutes(
          ZReferenceNumberPage,
          testOrganisationDetails,
          Some(OrganisationDetailsCya)
        )

      result shouldBe OrganisationDetailsCheckYourAnswersController.onPageLoad()
    }

    "route ZReferenceNumberPage to TradingUsingDifferentNamePage when no returnTo present" in {
      val result: Call =
        navigator.normalRoutes(
          ZReferenceNumberPage,
          testOrganisationDetails,
          None
        )

      result shouldBe TradingUsingDifferentNameController.onPageLoad(
        NormalMode,
        None
      )
    }

    "route TradingUsingDifferentNamePage to TradingNamePage when yes selected and returnTo is OrganisationDetailsCya" in {
      val answers =
        OrganisationDetails(tradingUsingDifferentName = Some(YesNoAnswer.Yes))

      val result: Call =
        navigator.normalRoutes(
          TradingUsingDifferentNamePage,
          answers,
          Some(OrganisationDetailsCya)
        )

      result shouldBe TradingNameController.onPageLoad(
        NormalMode,
        Some(OrganisationDetailsCya)
      )
    }

    "route TradingUsingDifferentNamePage to OrganisationDetails CYA when no selected and returnTo is OrganisationDetailsCya" in {
      val answers =
        OrganisationDetails(tradingUsingDifferentName = Some(YesNoAnswer.No))

      val result: Call =
        navigator.normalRoutes(
          TradingUsingDifferentNamePage,
          answers,
          Some(OrganisationDetailsCya)
        )

      result shouldBe OrganisationDetailsCheckYourAnswersController.onPageLoad()
    }

    "route TradingUsingDifferentNamePage to TaskList when no answer is present" in {
      val result: Call =
        navigator.normalRoutes(
          TradingUsingDifferentNamePage,
          OrganisationDetails(),
          None
        )

      result shouldBe TaskListController.onPageLoad()
    }

    "route TradingNamePage to OrganisationDetails CYA when returnTo is OrganisationDetailsCya" in {
      val result: Call =
        navigator.normalRoutes(
          TradingNamePage,
          OrganisationDetails(),
          Some(OrganisationDetailsCya)
        )

      result shouldBe OrganisationDetailsCheckYourAnswersController.onPageLoad()
    }

    "route TradingUsingDifferentNamePage to TradingNamePage when Yes selected" in {
      val answers = OrganisationDetails(tradingUsingDifferentName = Some(YesNoAnswer.Yes))

      val result: Call = navigator.normalRoutes(TradingUsingDifferentNamePage, answers, None)

      result shouldBe TradingNameController.onPageLoad(NormalMode, None)
    }

    "route TradingUsingDifferentNamePage to FirmReferenceNumberPage when No selected" in {
      val answers = OrganisationDetails(tradingUsingDifferentName = Some(YesNoAnswer.No))

      val result: Call = navigator.normalRoutes(TradingUsingDifferentNamePage, answers, None)

      result shouldBe FirmReferenceNumberController.onPageLoad(NormalMode)
    }

    "route TradingNamePage to FirmReferenceNumberPage" in {
      val result: Call = navigator.normalRoutes(TradingNamePage, OrganisationDetails(), None)
      result shouldBe FirmReferenceNumberController.onPageLoad(NormalMode)
    }

    "route IsaProductsPage  to IF products when IF ISA selected" in {
      val answers = answersWithIsaProducts(Seq(InnovativeFinanceIsas))

      val result: Call = navigator.normalRoutes(IsaProductsPage, answers, None)

      result shouldBe InnovativeFinancialProductsController.onPageLoad(NormalMode)
    }

    "route IsaProductsPage to CYA when IF ISA not selected" in {
      val answers = answersWithIsaProducts(Seq(CashIsas))

      val result: Call = navigator.normalRoutes(IsaProductsPage, answers, None)

      result shouldBe IsaProductsCheckYourAnswersController.onPageLoad()
    }

    "route IsaProductsPage to TaskList when isaProducts is missing" in {
      val answers = emptyIsaProductsAnswers.copy(isaProducts = None)

      val result: Call = navigator.normalRoutes(IsaProductsPage, answers, None)

      result shouldBe TaskListController.onPageLoad()
    }

    "route InnovativeFinancialProductsPage to P2P platform when 36H selected" in {
      val answers = answersWithInnovativeFinancialProducts(Seq(PeertopeerLoansUsingAPlatformWith36hPermissions))

      val result: Call = navigator.normalRoutes(InnovativeFinancialProductsPage, answers, None)

      result shouldBe PeerToPeerPlatformController.onPageLoad(NormalMode)
    }

    "route InnovativeFinancialProductsPage to CYA when 36H not selected" in {
      val answers = answersWithInnovativeFinancialProducts(Seq(CrowdFundedDebentures))

      val result: Call = navigator.normalRoutes(InnovativeFinancialProductsPage, answers, None)

      result shouldBe IsaProductsCheckYourAnswersController.onPageLoad()
    }

    "route InnovativeFinancialProductsPage to TaskList when innovativeFinancialProducts is missing" in {
      val answers = emptyIsaProductsAnswers.copy(innovativeFinancialProducts = None)

      val result: Call = navigator.normalRoutes(InnovativeFinancialProductsPage, answers, None)

      result shouldBe TaskListController.onPageLoad()
    }

    "route PeerToPeerPlatformPage to PeerToPeerPlatformNumberPage" in {
      val result: Call = navigator.normalRoutes(PeerToPeerPlatformPage, emptyIsaProductsAnswers, None)

      result shouldBe PeerToPeerPlatformNumberController.onPageLoad(NormalMode)
    }

    "route PeerToPeerPlatformNumberPage to ISA products CYA" in {
      val result: Call = navigator.normalRoutes(PeerToPeerPlatformNumberPage, emptyIsaProductsAnswers, None)

      result shouldBe IsaProductsCheckYourAnswersController.onPageLoad()
    }

    "route CertificatesOfAuthorityYesNoPage to FcaArticlesPage if yes submitted" in {
      val result: Call = navigator.normalRoutes(CertificatesOfAuthorityYesNoPage, coaAnswers, None)
      result shouldBe FcaArticlesController.onPageLoad(NormalMode)
    }

    "route CertificatesOfAuthorityYesNoPage to FinancialOrganisationPage if no submitted" in {
      val result: Call =
        navigator.normalRoutes(CertificatesOfAuthorityYesNoPage, coaAnswers.copy(certificatesYesNo = Some(No)), None)
      result shouldBe FinancialOrganisationController.onPageLoad(NormalMode)
    }

    "route to CertificatesOfAuthorityYesNoPage if no answer is present for certificatesYesNo" in {
      val result: Call =
        navigator.normalRoutes(CertificatesOfAuthorityYesNoPage, coaAnswers.copy(certificatesYesNo = None), None)
      result shouldBe CertificatesOfAuthorityYesNoController.onPageLoad(NormalMode)
    }

    "route RegisteredAddressCorrespondencePage to RegisteredAddressCorrespondenceController" in {
      val result: Call = navigator.normalRoutes(FirmReferenceNumberPage, testOrganisationDetails, None)
      result shouldBe RegisteredAddressCorrespondenceController.onPageLoad(NormalMode, None)
    }

    "route RegisteredAddressCorrespondencePage to OrganisationTelephoneNumberController if yes submitted" in {
      val result: Call = navigator.normalRoutes(
        RegisteredAddressCorrespondencePage,
        testOrganisationDetails.copy(registeredAddressCorrespondence = Some(YesNoAnswer.Yes)),
        None
      )
      result shouldBe OrganisationTelephoneNumberController.onPageLoad(NormalMode)
    }

    "route RegisteredAddressCorrespondencePage to Address Lookup page if no submitted" in {
      val result: Call =
        navigator.normalRoutes(
          RegisteredAddressCorrespondencePage,
          testOrganisationDetails.copy(registeredAddressCorrespondence = Some(YesNoAnswer.No)),
          None
        )
      result shouldBe AddAnotherAddressController.onPageLoad(NormalMode, None)
    }

    "route to RegisteredAddressCorrespondencePage if no answer is present for registeredAddressCorrespondence" in {
      val result: Call =
        navigator.normalRoutes(
          RegisteredAddressCorrespondencePage,
          testOrganisationDetails.copy(registeredAddressCorrespondence = None),
          None
        )
      result shouldBe TaskListController.onPageLoad()
    }

    "route to AddAnotherAddressPage if 1 address is persisted in user answers to the ConfirmAddressPage" in {
      val result: Call =
        navigator.normalRoutes(
          AddAnotherAddressPage,
          testOrganisationDetails.copy(addAnotherAddress =
            Some(
              AddAnotherAddress(
                postcode = testString,
                filter = Some(testString),
                addresses = Seq(
                  LookupAddress(
                    addressLine1 = Some(testString),
                    addressLine2 = Some(testString),
                    postCode = Some(testString)
                  )
                ),
                selectedAddress = None
              )
            )
          ),
          returnTo = None
        )
      result shouldBe ConfirmCorrespondenceAddressController.onPageLoad(None)
    }

    "route from AddAnotherAddressPage if multiple addresses are persisted in user answers to the SelectAddressPage" in {
      val result: Call =
        navigator.normalRoutes(
          AddAnotherAddressPage,
          testOrganisationDetails.copy(addAnotherAddress =
            Some(
              AddAnotherAddress(
                postcode = testString,
                filter = Some(testString),
                addresses = Seq(
                  LookupAddress(
                    addressLine1 = Some(testString),
                    addressLine2 = Some(testString),
                    postCode = Some(testString)
                  ),
                  LookupAddress(
                    addressLine1 = Some(testString),
                    addressLine2 = Some(testString),
                    postCode = Some(testString)
                  )
                ),
                selectedAddress = None
              )
            )
          ),
          returnTo = None
        )
      result shouldBe ChooseAddressController.onPageLoad(NormalMode, None)
    }

    "route AddAnotherAddressPage to TaskList when no addresses are present" in {
      val result: Call =
        navigator.normalRoutes(
          AddAnotherAddressPage,
          testOrganisationDetails.copy(
            addAnotherAddress = Some(
              AddAnotherAddress(
                postcode = testString,
                filter = Some(testString),
                addresses = Seq.empty,
                selectedAddress = None
              )
            )
          ),
          None
        )

      result shouldBe TaskListController.onPageLoad()
    }

    "route AddAnotherAddressPage to ConfirmCorrespondenceAddressController with returnTo param when one address exists" in {
      val result: Call =
        navigator.normalRoutes(
          AddAnotherAddressPage,
          testOrganisationDetails.copy(
            addAnotherAddress = Some(
              AddAnotherAddress(
                postcode = testString,
                filter = Some(testString),
                addresses = Seq(
                  LookupAddress(
                    addressLine1 = Some(testString),
                    addressLine2 = Some(testString),
                    postCode = Some(testString)
                  )
                ),
                selectedAddress = None
              )
            )
          ),
          Some(OrganisationDetailsCya)
        )

      result shouldBe ConfirmCorrespondenceAddressController.onPageLoad(
        Some(OrganisationDetailsCya)
      )
    }

    "route AddAnotherAddressPage to ChooseAddressController with returnTo param when multiple addresses exist" in {
      val result: Call =
        navigator.normalRoutes(
          AddAnotherAddressPage,
          testOrganisationDetails.copy(
            addAnotherAddress = Some(
              AddAnotherAddress(
                postcode = testString,
                filter = Some(testString),
                addresses = Seq(
                  LookupAddress(
                    addressLine1 = Some(testString),
                    addressLine2 = Some(testString),
                    postCode = Some(testString)
                  ),
                  LookupAddress(
                    addressLine1 = Some(testString),
                    addressLine2 = Some(testString),
                    postCode = Some(testString)
                  )
                ),
                selectedAddress = None
              )
            )
          ),
          Some(OrganisationDetailsCya)
        )

      result shouldBe ChooseAddressController.onPageLoad(
        NormalMode,
        Some(OrganisationDetailsCya)
      )
    }

    "route ChooseAddressPage to TaskList when no address selection is present" in {
      val result: Call =
        navigator.normalRoutes(
          ChooseAddressPage,
          testOrganisationDetails.copy(
            addAnotherAddress = Some(
              AddAnotherAddress(
                postcode = testString,
                filter = Some(testString),
                addresses = Seq(
                  LookupAddress(
                    addressLine1 = Some(testString),
                    addressLine2 = Some(testString),
                    postCode = Some(testString)
                  )
                ),
                selectedAddress = None
              )
            )
          ),
          None
        )

      result shouldBe TaskListController.onPageLoad()
    }

    "route ChooseAddressPage to EnterYourOrganisationAddressController with returnTo param when manual entry selected" in {
      val result: Call =
        navigator.normalRoutes(
          ChooseAddressPage,
          testOrganisationDetails.copy(
            addAnotherAddress = Some(
              AddAnotherAddress(
                postcode = testString,
                filter = Some(testString),
                addresses = Seq(
                  LookupAddress(
                    addressLine1 = Some(testString),
                    addressLine2 = Some(testString),
                    postCode = Some(testString)
                  )
                ),
                selectedAddress = Some(ManualEntry)
              )
            )
          ),
          Some(OrganisationDetailsCya)
        )

      result shouldBe EnterYourOrganisationAddressController.onPageLoad(
        NormalMode,
        Some(OrganisationDetailsCya)
      )
    }

    "route ChooseAddressPage to ConfirmCorrespondenceAddressController with returnTo param when address selected" in {
      val result: Call =
        navigator.normalRoutes(
          ChooseAddressPage,
          testOrganisationDetails.copy(
            addAnotherAddress = Some(
              AddAnotherAddress(
                postcode = testString,
                filter = Some(testString),
                addresses = Seq(
                  LookupAddress(
                    addressLine1 = Some(testString),
                    addressLine2 = Some(testString),
                    postCode = Some(testString)
                  )
                ),
                selectedAddress = Some(SelectedCorrespondenceAddress.Address(0))
              )
            )
          ),
          Some(OrganisationDetailsCya)
        )

      result shouldBe ConfirmCorrespondenceAddressController.onPageLoad(
        Some(OrganisationDetailsCya)
      )
    }

    "route ConfirmAddressPage to OrganisationDetails CYA when returnTo is OrganisationDetailsCya" in {
      val result: Call =
        navigator.normalRoutes(
          ConfirmAddressPage,
          testOrganisationDetails,
          Some(OrganisationDetailsCya)
        )

      result shouldBe OrganisationDetailsCheckYourAnswersController.onPageLoad()
    }

    "route ConfirmAddressPage to OrganisationTelephoneNumberController when no returnTo present" in {
      val result: Call =
        navigator.normalRoutes(
          ConfirmAddressPage,
          testOrganisationDetails,
          None
        )

      result shouldBe OrganisationTelephoneNumberController.onPageLoad(
        NormalMode
      )
    }

    "route ProductsManagedByThirdPartyPage to TaskList when no answer present" in {
      val result: Call =
        navigator.normalRoutes(
          ProductsManagedByThirdPartyPage,
          ThirdPartyOrganisations(None),
          None
        )

      result shouldBe TaskListController.onPageLoad()
    }

    "route InvestorFundsUsedByThirdPartyPage to TaskList when third party id not found" in {
      val result: Call =
        navigator.normalRoutes(
          InvestorFundsUsedByThirdPartyPage(testString),
          ThirdPartyOrganisations(
            Some(YesNoAnswer.Yes),
            Seq(
              ThirdParty("different-id")
            )
          ),
          None
        )

      result shouldBe TaskListController.onPageLoad()
    }

    "route to ManualAddressEntryPage if none is selected and persisted in user answers" in {
      val result: Call =
        navigator.normalRoutes(
          ChooseAddressPage,
          testOrganisationDetails.copy(addAnotherAddress =
            Some(
              AddAnotherAddress(
                postcode = testString,
                filter = Some(testString),
                addresses = Seq(
                  LookupAddress(
                    addressLine1 = Some(testString),
                    addressLine2 = Some(testString),
                    postCode = Some(testString)
                  ),
                  LookupAddress(
                    addressLine1 = Some(testString),
                    addressLine2 = Some(testString),
                    postCode = Some(testString)
                  )
                ),
                selectedAddress = Some(ManualEntry)
              )
            )
          ),
          returnTo = None
        )

      result shouldBe EnterYourOrganisationAddressController.onPageLoad(NormalMode, None)
    }

    "route to ConfirmAddressPage if an address is selected and persisted in user answers" in {
      val result: Call =
        navigator.normalRoutes(
          ChooseAddressPage,
          testOrganisationDetails.copy(addAnotherAddress =
            Some(
              AddAnotherAddress(
                postcode = testString,
                filter = Some(testString),
                addresses = Seq(
                  LookupAddress(
                    addressLine1 = Some(testString),
                    addressLine2 = Some(testString),
                    postCode = Some(testString)
                  ),
                  LookupAddress(
                    addressLine1 = Some(testString),
                    addressLine2 = Some(testString),
                    postCode = Some(testString)
                  )
                ),
                selectedAddress = Some(SelectedCorrespondenceAddress.Address(0))
              )
            )
          ),
          returnTo = None
        )
      result shouldBe ConfirmCorrespondenceAddressController.onPageLoad(None)
    }

    "ChooseAddressPage route to ManualAddressEntryPage if none is selected and persisted in user answers" in {
      val result: Call =
        navigator.normalRoutes(
          ChooseAddressPage,
          testOrganisationDetails.copy(addAnotherAddress =
            Some(
              AddAnotherAddress(
                postcode = testString,
                filter = Some(testString),
                addresses = Seq(
                  LookupAddress(
                    addressLine1 = Some(testString),
                    addressLine2 = Some(testString),
                    postCode = Some(testString)
                  ),
                  LookupAddress(
                    addressLine1 = Some(testString),
                    addressLine2 = Some(testString),
                    postCode = Some(testString)
                  )
                ),
                selectedAddress = Some(ManualEntry)
              )
            )
          ),
          returnTo = None
        )

      result shouldBe EnterYourOrganisationAddressController.onPageLoad(NormalMode, None)
    }

    "ChooseAddressPage route to ConfirmAddressPage if an address is selected and persisted in user answers" in {
      val result: Call =
        navigator.normalRoutes(
          ChooseAddressPage,
          testOrganisationDetails.copy(addAnotherAddress =
            Some(
              AddAnotherAddress(
                postcode = testString,
                filter = Some(testString),
                addresses = Seq(
                  LookupAddress(
                    addressLine1 = Some(testString),
                    addressLine2 = Some(testString),
                    postCode = Some(testString)
                  ),
                  LookupAddress(
                    addressLine1 = Some(testString),
                    addressLine2 = Some(testString),
                    postCode = Some(testString)
                  )
                ),
                selectedAddress = Some(SelectedCorrespondenceAddress.Address(0))
              )
            )
          ),
          returnTo = None
        )
      result shouldBe ConfirmCorrespondenceAddressController.onPageLoad(None)
    }

    "route FcaArticlesPage to ISA products CYA" in {
      val result: Call = navigator.normalRoutes(FcaArticlesPage, coaAnswers, None)
      result shouldBe CoaCheckYourAnswersController.onPageLoad()
    }

    "route FinancialOrganisationPage to ISA products CYA" in {
      val result: Call = navigator.normalRoutes(FinancialOrganisationPage, coaAnswers, None)
      result shouldBe CoaCheckYourAnswersController.onPageLoad()
    }

    "route OrganisationEmailAddressPage to OrganisationEmailVerificationCodePage" in {
      val result: Call = navigator.normalRoutes(OrganisationEmailAddressPage, OrganisationEmail(Some(testString)), None)
      result shouldBe EmailVerificationCodeController.onPageLoad(NormalMode, None)
    }

    "route OrganisationEmailVerificationCodePage to OrganisationEmail CYA" in {
      val result: Call =
        navigator.normalRoutes(
          OrganisationEmailVerificationCodePage,
          OrganisationEmail(Some(testString), Some(true)),
          None
        )
      result shouldBe OrganisationEmailCyaController.onPageLoad()
    }

    "route RemoveSignatoryPage to AddedSignatoryController when signatory exists in journeyAnswers" in {
      val result: Call = navigator.normalRoutes(RemoveSignatoryPage(signatoryId), signatoriesAnswers, None)
      result shouldBe AddedSignatoryController.onPageLoad(NormalMode, None)
    }

    "route RemoveSignatoryPage to AddedSignatoryController when a signatory doesn't exists in journeyAnswers" in {
      val result: Call = navigator.normalRoutes(RemoveSignatoryPage(signatoryId), Signatories(Seq.empty), None)
      result shouldBe AddASignatoryController.onPageLoad()
    }

    "route SignatoryNamePage to SignatoryJobTitleController" in {
      val result: Call = navigator.normalRoutes(SignatoryNamePage(signatoryId), signatoriesAnswers, None)
      result shouldBe SignatoryJobTitleController.onPageLoad(id = signatoryId, mode = NormalMode)
    }

    "route SignatoryJobTitlePage to SignatoryCheckYourAnswersController" in {
      val result: Call = navigator.normalRoutes(SignatoryJobTitlePage(signatoryId), signatoriesAnswers, None)
      result shouldBe SignatoryCheckYourAnswersController.onPageLoad(id = signatoryId)
    }

    "route LiaisonOfficerNamePage to LiaisonOfficerEmailController" in {
      val result: Call = navigator.normalRoutes(LiaisonOfficerNamePage(testString), liaisonOfficersAnswers, None)

      result shouldBe LiaisonOfficerEmailController.onPageLoad(testString, NormalMode)
    }

    "route LiaisonOfficerEmailPage to LiaisonOfficerPhoneNumberController" in {
      val result: Call = navigator.normalRoutes(LiaisonOfficerEmailPage(testString), liaisonOfficersAnswers, None)

      result shouldBe LiaisonOfficerPhoneNumberController.onPageLoad(testString, NormalMode)
    }

    "route LiaisonOfficerPhoneNumberPage to LiaisonOfficerCommunicationController" in {
      val result: Call = navigator.normalRoutes(LiaisonOfficerPhoneNumberPage(testString), liaisonOfficersAnswers, None)

      result shouldBe LiaisonOfficerCommunicationController.onPageLoad(testString, NormalMode)
    }

    "route LiaisonOfficerCommunicationPage to LO CYA" in {
      val result: Call =
        navigator.normalRoutes(LiaisonOfficerCommunicationPage(testString), liaisonOfficersAnswers, None)

      result shouldBe LoCheckYourAnswersController.onPageLoad(testString)
    }

    "route RemoveLiaisonOfficerPage to AddLiaisonOfficerController when no liaison officers remain" in {
      val result: Call = navigator.normalRoutes(RemoveLiaisonOfficerPage, LiaisonOfficers(Nil), None)

      result shouldBe AddLiaisonOfficerController.onPageLoad()
    }

    "route RemoveLiaisonOfficerPage to AddedLiaisonOfficersController when liaison officers remain" in {
      val result: Call = navigator.normalRoutes(RemoveLiaisonOfficerPage, liaisonOfficersAnswers, None)

      result shouldBe AddedLiaisonOfficersController.onPageLoad(NormalMode, None)
    }

    "route ProductsManagedByThirdParty to TaskList when answer is no" in {
      val result: Call =
        navigator.normalRoutes(ProductsManagedByThirdPartyPage, ThirdPartyOrganisations(Some(YesNoAnswer.No)), None)

      result shouldBe TaskListController.onPageLoad()
    }

    "route ProductsManagedByThirdParty to ThirdPartyOrgDetailsPage when answer is yes" in {
      val result: Call =
        navigator.normalRoutes(ProductsManagedByThirdPartyPage, ThirdPartyOrganisations(Some(YesNoAnswer.Yes)), None)

      result shouldBe ThirdPartyOrgDetailsController.onPageLoad(id = None, mode = NormalMode, returnTo = None)
    }

    "route ThirdPartyOrgDetailsPage to ReturnsManagedByThirdParty" in {
      val result: Call = navigator.normalRoutes(
        ThirdPartyOrgDetailsPage(testString),
        ThirdPartyOrganisations(Some(YesNoAnswer.No), Seq(ThirdParty(testString))),
        None
      )

      result shouldBe ThirdPartyManagingReturnsController.onPageLoad(testString, NormalMode, None)
    }

    "route ThirdPartyManagingReturnsPage to InvestorFundsUsedByThirdPartyController when 'yes' is submitted" in {
      val result: Call = navigator.normalRoutes(
        ThirdPartyManagingReturnsPage(testString),
        ThirdPartyOrganisations(
          Some(YesNoAnswer.No),
          Seq(ThirdParty(testString, Some(testString), None, Some(YesNoAnswer.Yes)))
        ),
        None
      )

      result shouldBe InvestorFundsUsedByThirdPartyController.onPageLoad(testString, NormalMode, None)
    }

    "route ThirdPartyManagingReturnsPage to TaskList when 'no' is submitted" in {
      val result: Call = navigator.normalRoutes(
        ThirdPartyManagingReturnsPage(testString),
        ThirdPartyOrganisations(
          Some(YesNoAnswer.No),
          Seq(ThirdParty(testString, Some(testString), None, Some(YesNoAnswer.No)))
        ),
        None
      )

      result shouldBe InvestorFundsUsedByThirdPartyController.onPageLoad(
        id = testString,
        mode = NormalMode,
        returnTo = None
      )
    }

    "route InvestorFundsUsedByThirdPartyPage to TaskList when 'yes' is submitted" in {
      val result: Call = navigator.normalRoutes(
        InvestorFundsUsedByThirdPartyPage(testString),
        ThirdPartyOrganisations(
          Some(YesNoAnswer.No),
          Seq(ThirdParty(testString, Some(testString), None, Some(YesNoAnswer.Yes), Some(YesNoAnswer.Yes)))
        ),
        None
      )

      result shouldBe ThirdPartyInvestorFundsPercentageController.onPageLoad(
        id = testString,
        mode = NormalMode,
        returnTo = None
      )
    }

    "route InvestorFundsUsedByThirdPartyPage to ThirdPartyCheckYourAnswersController when 'no' is submitted" in {
      val result: Call = navigator.normalRoutes(
        InvestorFundsUsedByThirdPartyPage(testString),
        ThirdPartyOrganisations(
          Some(YesNoAnswer.No),
          Seq(ThirdParty(testString, Some(testString), None, Some(YesNoAnswer.Yes), Some(YesNoAnswer.No)))
        ),
        None
      )

      result shouldBe ThirdPartyCheckYourAnswersController.onPageLoad(id = testString)
    }

    "route InvestorFundsUsedByThirdPartyPage to TaskList when no answer is present" in {
      val result: Call = navigator.normalRoutes(
        InvestorFundsUsedByThirdPartyPage(testString),
        ThirdPartyOrganisations(
          Some(YesNoAnswer.Yes),
          Seq(ThirdParty(testString, Some(testString), None, Some(YesNoAnswer.Yes), None))
        ),
        None
      )

      result shouldBe TaskListController.onPageLoad()
    }

    "route ThirdPartyInvestorFundsPercentagePage to ThirdPartyCheckYourAnswersController" in {
      val result: Call = navigator.normalRoutes(
        ThirdPartyInvestorFundsPercentagePage(testString),
        ThirdPartyOrganisations(
          Some(YesNoAnswer.No),
          Seq(ThirdParty(testString, Some(testString), None, Some(YesNoAnswer.Yes), Some(YesNoAnswer.No), Some("20")))
        ),
        None
      )
      result shouldBe ThirdPartyCheckYourAnswersController.onPageLoad(id = testString)
    }

    "route RemoveThirdPartyPage to TaskList when third parties exist in answers" in {
      val result: Call = navigator.normalRoutes(
        RemoveThirdPartyPage,
        ThirdPartyOrganisations(
          Some(YesNoAnswer.Yes),
          Seq(ThirdParty(testString, Some(testString), None, Some(YesNoAnswer.Yes), Some(YesNoAnswer.No)))
        ),
        None
      )

      result shouldBe AddedThirdPartiesController.onPageLoad(NormalMode, None)
    }

    "route RemoveThirdPartyPage to ProductsManagedByThirdPartyPage when no third parties exist in answers" in {
      val result: Call =
        navigator.normalRoutes(RemoveThirdPartyPage, ThirdPartyOrganisations(Some(YesNoAnswer.Yes), Seq.empty), None)

      result shouldBe ProductsManagedByThirdPartyController.onPageLoad(NormalMode)
    }

    "route unknown page to TaskList" in {
      case object UnknownPage extends PageWithoutDependents[IsaProducts] {
        override def clearAnswer(answers: IsaProducts): IsaProducts = answers
      }

      assertThrows[NotImplementedError] {
        navigator.normalRoutes(UnknownPage, emptyIsaProductsAnswers, None)
      }
    }

    "route ThirdPartyInvestorFundsPercentagePage to ThirdParty CYA with returnTo when SubmissionCyaViaAddedThirdParties" in {

      val result: Call =
        navigator.normalRoutes(
          ThirdPartyInvestorFundsPercentagePage(testString),
          ThirdPartyOrganisations(),
          Some(SubmissionCyaViaAddedThirdParties)
        )

      result shouldBe ThirdPartyCheckYourAnswersController.onPageLoad(
        testString,
        Some(SubmissionCyaViaAddedThirdParties)
      )
    }

    "route ThirdPartyInvestorFundsPercentagePage to ThirdParty CYA with returnTo when MultipleThirdPartiesCyaViaAddedThirdParties" in {

      val result: Call =
        navigator.normalRoutes(
          ThirdPartyInvestorFundsPercentagePage(testString),
          ThirdPartyOrganisations(),
          Some(MultipleThirdPartiesCyaViaAddedThirdParties)
        )

      result shouldBe ThirdPartyCheckYourAnswersController.onPageLoad(
        testString,
        Some(MultipleThirdPartiesCyaViaAddedThirdParties)
      )
    }

    "route ThirdPartyInvestorFundsPercentagePage to Submission CYA when returnTo = SubmissionCya" in {

      val result: Call =
        navigator.normalRoutes(
          ThirdPartyInvestorFundsPercentagePage(testString),
          ThirdPartyOrganisations(),
          Some(SubmissionCya)
        )

      result shouldBe SubmissionCyaController.onPageLoad()
    }

    "route ThirdPartyInvestorFundsPercentagePage to Third Parties CYA when returnTo = MultipleThirdPartiesCya" in {

      val result: Call =
        navigator.normalRoutes(
          ThirdPartyInvestorFundsPercentagePage(testString),
          ThirdPartyOrganisations(),
          Some(MultipleThirdPartiesCya)
        )

      result shouldBe ThirdPartiesCheckYourAnswersController.onPageLoad()
    }

    "route ThirdPartyInvestorFundsPercentagePage to default ThirdParty CYA when no returnTo provided" in {

      val result: Call =
        navigator.normalRoutes(
          ThirdPartyInvestorFundsPercentagePage(testString),
          ThirdPartyOrganisations(),
          None
        )

      result shouldBe ThirdPartyCheckYourAnswersController.onPageLoad(
        testString,
        None
      )
    }
  }

  "Navigator.checkRouteMap" - {

    "route RegisteredIsaManagerPage to Organisation Details CYA" in {
      navigator.checkRouteMap(RegisteredIsaManagerPage, None) shouldBe
        OrganisationDetailsCheckYourAnswersController.onPageLoad()
    }

    "route ZReferenceNumberPage to Organisation Details CYA" in {
      navigator.checkRouteMap(ZReferenceNumberPage, None) shouldBe
        OrganisationDetailsCheckYourAnswersController.onPageLoad()
    }

    "route TradingUsingDifferentNamePage to Organisation Details CYA" in {
      navigator.checkRouteMap(TradingUsingDifferentNamePage, None) shouldBe
        OrganisationDetailsCheckYourAnswersController.onPageLoad()
    }

    "route TradingNamePage to Organisation Details CYA" in {
      navigator.checkRouteMap(TradingNamePage, None) shouldBe
        OrganisationDetailsCheckYourAnswersController.onPageLoad()
    }

    "route FirmReferenceNumberPage to Organisation Details CYA" in {
      navigator.checkRouteMap(FirmReferenceNumberPage, None) shouldBe
        OrganisationDetailsCheckYourAnswersController.onPageLoad()
    }

    "route EnterYourOrganisationAddressPage to Organisation Details CYA" in {
      navigator.checkRouteMap(EnterYourOrganisationAddressPage, None) shouldBe
        OrganisationDetailsCheckYourAnswersController.onPageLoad()
    }

    "route OrganisationTelephoneNumberPage to Organisation Details CYA" in {
      navigator.checkRouteMap(OrganisationTelephoneNumberPage, None) shouldBe
        OrganisationDetailsCheckYourAnswersController.onPageLoad()
    }

    "route IsaProductsPage to ISA products CYA" in {
      navigator.checkRouteMap(IsaProductsPage, None) shouldBe IsaProductsCheckYourAnswersController.onPageLoad()
    }

    "route InnovativeFinancialProductsPage to ISA products CYA" in {
      navigator.checkRouteMap(InnovativeFinancialProductsPage, None) shouldBe IsaProductsCheckYourAnswersController
        .onPageLoad()
    }

    "route PeerToPeerPlatformPage to ISA products CYA" in {
      navigator.checkRouteMap(PeerToPeerPlatformPage, None) shouldBe IsaProductsCheckYourAnswersController.onPageLoad()
    }

    "route PeerToPeerPlatformNumberPage to ISA products CYA" in {
      navigator.checkRouteMap(PeerToPeerPlatformNumberPage, None) shouldBe IsaProductsCheckYourAnswersController
        .onPageLoad()
    }

    "route CertificatesOfAuthorityYesNoPage to COA CYA" in {
      navigator.checkRouteMap(CertificatesOfAuthorityYesNoPage, None) shouldBe
        CoaCheckYourAnswersController.onPageLoad()
    }

    "route FcaArticlesPage to COA CYA" in {
      navigator.checkRouteMap(FcaArticlesPage, None) shouldBe
        CoaCheckYourAnswersController.onPageLoad()
    }

    "route FinancialOrganisationPage to COA CYA" in {
      navigator.checkRouteMap(FinancialOrganisationPage, None) shouldBe
        CoaCheckYourAnswersController.onPageLoad()
    }

    "route RegisteredAddressCorrespondencePage to COA CYA" in {
      navigator.checkRouteMap(RegisteredAddressCorrespondencePage, None) shouldBe
        OrganisationDetailsCheckYourAnswersController.onPageLoad()
    }

    "route OrganisationEmailAddressPage to Org email CYA" in {
      navigator.checkRouteMap(OrganisationEmailAddressPage, None) shouldBe
        OrganisationEmailCyaController.onPageLoad()
    }

    "route SignatoryNamePage to SignatoryCheckYourAnswersController" in {
      navigator.checkRouteMap(SignatoryNamePage(signatoryId), None) shouldBe
        SignatoryCheckYourAnswersController.onPageLoad(id = signatoryId)
    }

    "route SignatoryJobTitlePage to SignatoryCheckYourAnswersController" in {
      navigator.checkRouteMap(SignatoryJobTitlePage(signatoryId), None) shouldBe
        SignatoryCheckYourAnswersController.onPageLoad(id = signatoryId)
    }

    "route LiaisonOfficerNamePage to LO CYA" in {
      navigator.checkRouteMap(LiaisonOfficerNamePage(testString), None) shouldBe
        LoCheckYourAnswersController.onPageLoad(testString)
    }

    "route LiaisonOfficerEmailPage to LO CYA" in {
      navigator.checkRouteMap(LiaisonOfficerEmailPage(testString), None) shouldBe
        LoCheckYourAnswersController.onPageLoad(testString)
    }

    "route LiaisonOfficerPhoneNumberPage to LO CYA" in {
      navigator.checkRouteMap(LiaisonOfficerPhoneNumberPage(testString), None) shouldBe
        LoCheckYourAnswersController.onPageLoad(testString)
    }

    "route LiaisonOfficerCommunicationPage to LO CYA" in {
      navigator.checkRouteMap(LiaisonOfficerCommunicationPage(testString), None) shouldBe
        LoCheckYourAnswersController.onPageLoad(testString)
    }

    "route ProductsManagedByThirdPartyPage to Third Parties CYA" in {
      navigator.checkRouteMap(ProductsManagedByThirdPartyPage, None) shouldBe
        ThirdPartiesCheckYourAnswersController.onPageLoad()
    }

    "route ThirdPartyOrgDetailsPage to Final Third Party CYA if ReturnTo param passed" in {
      navigator.checkRouteMap(ThirdPartyOrgDetailsPage(testString), Some(MultipleThirdPartiesCya)) shouldBe
        ThirdPartiesCheckYourAnswersController.onPageLoad()
    }

    "route ThirdPartyOrgDetailsPage to Third Party CYA" in {
      navigator.checkRouteMap(ThirdPartyOrgDetailsPage(testString), None) shouldBe
        ThirdPartyCheckYourAnswersController.onPageLoad(testString)
    }

    "route ThirdPartyManagingReturnsPage to Third Party CYA" in {
      navigator.checkRouteMap(ThirdPartyManagingReturnsPage(testString), None) shouldBe
        ThirdPartyCheckYourAnswersController.onPageLoad(testString)
    }

    "route ThirdPartyManagingReturnsPage to Final Third Party CYA if ReturnTo param passed" in {
      navigator.checkRouteMap(ThirdPartyManagingReturnsPage(testString), Some(MultipleThirdPartiesCya)) shouldBe
        ThirdPartiesCheckYourAnswersController.onPageLoad()
    }

    "route InvestorFundsUsedByThirdPartyPage to Third Party CYA" in {
      navigator.checkRouteMap(InvestorFundsUsedByThirdPartyPage(testString), None) shouldBe
        ThirdPartyCheckYourAnswersController.onPageLoad(testString)
    }

    "route InvestorFundsUsedByThirdPartyPage to Final Third Party CYA if ReturnTo param passed" in {
      navigator.checkRouteMap(InvestorFundsUsedByThirdPartyPage(testString), Some(MultipleThirdPartiesCya)) shouldBe
        ThirdPartiesCheckYourAnswersController.onPageLoad()
    }

    "route ThirdPartyInvestorFundsPercentagePage to Third Party CYA" in {
      navigator.checkRouteMap(ThirdPartyInvestorFundsPercentagePage(testString), None) shouldBe
        ThirdPartyCheckYourAnswersController.onPageLoad(testString)
    }

    "route ThirdPartyInvestorFundsPercentagePage to Final Third Party CYA if ReturnTo param passed" in {
      navigator.checkRouteMap(ThirdPartyInvestorFundsPercentagePage(testString), Some(MultipleThirdPartiesCya)) shouldBe
        ThirdPartiesCheckYourAnswersController.onPageLoad()
    }

    "route ThirdPartyConnectedOrganisationsPage to Third Parties CYA" in {
      navigator.checkRouteMap(ThirdPartyConnectedOrganisationsPage, None) shouldBe
        ThirdPartiesCheckYourAnswersController.onPageLoad()
    }

    "route unknown page to TaskList" in {
      case object UnknownPage extends PageWithoutDependents[IsaProducts] {
        override def clearAnswer(sectionAnswers: IsaProducts): IsaProducts = sectionAnswers
      }

      assertThrows[NotImplementedError] {
        navigator.normalRoutes(UnknownPage, emptyIsaProductsAnswers, None)
      }
    }
  }

  "liaisonOfficerCheckRoute" - {

    "must go to LO check answers when no returnTo is provided" in {
      val result =
        navigator.nextPageFromAddedLiaisonOfficers(YesNoAnswer.Yes, NormalMode, None)

      result mustEqual LiaisonOfficerNameController.onPageLoad(None, NormalMode, None)
    }

    "must return to Submission CYA when returnTo = SubmissionCya" in {
      val result =
        navigator.nextPageFromAddedLiaisonOfficers(YesNoAnswer.No, NormalMode, Some(SubmissionCya))

      result mustEqual SubmissionCyaController.onPageLoad()
    }

    "must return to Organisation Details CYA when returnTo = OrganisationDetailsCya" in {
      val result =
        navigator.nextPageFromAddedLiaisonOfficers(YesNoAnswer.No, NormalMode, Some(OrganisationDetailsCya))

      result mustEqual OrganisationDetailsCheckYourAnswersController.onPageLoad()
    }
  }

  "signatoryCheckRoute" - {

    "must go to Signatory check answers when no returnTo is provided" in {
      val result =
        navigator.nextPageFromAddedSignatories(YesNoAnswer.Yes, NormalMode, None)

      result mustEqual SignatoryNameController.onPageLoad(None, NormalMode, None)
    }

    "must return to Submission CYA when returnTo = SubmissionCya" in {
      val result =
        navigator.nextPageFromAddedSignatories(YesNoAnswer.No, NormalMode, Some(SubmissionCya))

      result mustEqual SubmissionCyaController.onPageLoad()
    }

    "must return to Organisation Details CYA when returnTo = OrganisationDetailsCya" in {
      val result =
        navigator.nextPageFromAddedSignatories(YesNoAnswer.No, NormalMode, Some(OrganisationDetailsCya))

      result mustEqual OrganisationDetailsCheckYourAnswersController.onPageLoad()
    }
  }

  "thirdPartyCheckModeRoute" - {

    "must go to Third Party check answers when no returnTo is provided" in {
      val result =
        navigator.nextPageFromAddedThirdParties(
          YesNoAnswer.Yes,
          count = 1,
          connectedOrganisations = Nil,
          mode = NormalMode,
          returnTo = None
        )

      result mustEqual ThirdPartyOrgDetailsController.onPageLoad(id = None, mode = NormalMode, returnTo = None)
    }

    "must return to Submission CYA when returnTo = SubmissionCya" in {
      val result =
        navigator.nextPageFromAddedThirdParties(
          YesNoAnswer.No,
          count = 1,
          connectedOrganisations = Nil,
          mode = NormalMode,
          returnTo = Some(SubmissionCya)
        )

      result mustEqual SubmissionCyaController.onPageLoad()
    }

    "must return to Organisation Details CYA when returnTo = OrganisationDetailsCya" in {
      val result =
        navigator.nextPageFromAddedThirdParties(
          YesNoAnswer.No,
          count = 1,
          connectedOrganisations = Nil,
          mode = NormalMode,
          returnTo = Some(OrganisationDetailsCya)
        )

      result mustEqual OrganisationDetailsCheckYourAnswersController.onPageLoad()
    }
  }

  "returnToRoute" - {

    "must redirect to Submission CYA when returnTo = SubmissionCyaViaAddedThirdParties" in {
      val result =
        navigator.nextPageFromAddedThirdParties(
          YesNoAnswer.No,
          count = 1,
          connectedOrganisations = Nil,
          mode = NormalMode,
          returnTo = Some(SubmissionCyaViaAddedThirdParties)
        )

      result mustEqual ThirdPartyConnectedOrganisationsController.onPageLoad(
        NormalMode,
        Some(SubmissionCyaViaAddedThirdParties)
      )
    }

    "must redirect to Submission CYA when returnTo = SubmissionCya" in {
      val result =
        navigator.nextPageFromAddedSignatories(
          YesNoAnswer.No,
          NormalMode,
          Some(SubmissionCya)
        )

      result mustEqual SubmissionCyaController.onPageLoad()
    }

    "must redirect to Third Parties CYA when returnTo = MultipleThirdPartiesCya" in {
      val result =
        navigator.nextPageFromAddedThirdParties(
          YesNoAnswer.No,
          count = 2,
          connectedOrganisations = Seq(testString, testString),
          mode = NormalMode,
          returnTo = Some(MultipleThirdPartiesCya)
        )

      result mustEqual ThirdPartiesCheckYourAnswersController.onPageLoad()
    }

    "must redirect to Organisation Details CYA when returnTo = OrganisationDetailsCya" in {
      val result =
        navigator.nextPageFromAddedLiaisonOfficers(
          YesNoAnswer.No,
          NormalMode,
          Some(OrganisationDetailsCya)
        )

      result mustEqual OrganisationDetailsCheckYourAnswersController.onPageLoad()
    }

    "must fall back to default route when returnTo is None" in {
      val result =
        navigator.nextPageFromAddedLiaisonOfficers(
          YesNoAnswer.No,
          NormalMode,
          None
        )

      result mustEqual TaskListController.onPageLoad()
    }
  }

  "nextPageFromAddedThirdParties" - {

    "must route to Submission CYA when returnTo = SubmissionCya" in {

      val result =
        navigator.nextPageFromAddedThirdParties(
          YesNoAnswer.No,
          count = 1,
          connectedOrganisations = Nil,
          mode = NormalMode,
          returnTo = Some(SubmissionCya)
        )

      result mustEqual SubmissionCyaController.onPageLoad()
    }

    "must route to Third Parties CYA when returnTo = MultipleThirdPartiesCya" in {

      val result =
        navigator.nextPageFromAddedThirdParties(
          YesNoAnswer.No,
          count = 1,
          connectedOrganisations = Nil,
          mode = NormalMode,
          returnTo = Some(MultipleThirdPartiesCya)
        )

      result mustEqual ThirdPartiesCheckYourAnswersController.onPageLoad()
    }
  }

  "addedThirdPartiesNextPage" - {

    "must go to ThirdPartyConnectedOrganisations when No and count > 1 and no connected organisations" in {

      val result =
        navigator.nextPageFromAddedThirdParties(
          answer = YesNoAnswer.No,
          count = 2,
          connectedOrganisations = Nil,
          mode = NormalMode,
          returnTo = None
        )

      result mustEqual ThirdPartyConnectedOrganisationsController.onPageLoad(NormalMode, None)
    }

    "must go to TaskList when No and any third party is in progress" in {

      val result =
        navigator.nextPageFromAddedThirdParties(
          answer = YesNoAnswer.No,
          count = 2,
          connectedOrganisations = Nil,
          mode = NormalMode,
          returnTo = None,
          hasInProgressItems = true
        )

      result mustEqual TaskListController.onPageLoad()
    }

    "must go to ThirdPartyConnectedOrganisations when No and returnTo = SubmissionCyaViaAddedThirdParties" in {

      val result =
        navigator.nextPageFromAddedThirdParties(
          answer = YesNoAnswer.No,
          count = 2,
          connectedOrganisations = Seq(testString, testString),
          mode = NormalMode,
          returnTo = Some(SubmissionCyaViaAddedThirdParties)
        )

      result mustEqual ThirdPartyConnectedOrganisationsController.onPageLoad(
        NormalMode,
        Some(SubmissionCyaViaAddedThirdParties)
      )
    }

    "must use default NO routing when No and no special conditions apply" in {

      val result =
        navigator.nextPageFromAddedThirdParties(
          answer = YesNoAnswer.No,
          count = 1,
          connectedOrganisations = Nil,
          mode = NormalMode,
          returnTo = None
        )

      result mustEqual TaskListController.onPageLoad()
    }

    "must go to Third Party Org Details with SubmissionCya context when Yes and returnTo = SubmissionCya" in {

      val result =
        navigator.nextPageFromAddedThirdParties(
          answer = YesNoAnswer.Yes,
          count = 1,
          connectedOrganisations = Nil,
          mode = NormalMode,
          returnTo = Some(SubmissionCya)
        )

      result mustEqual ThirdPartyOrgDetailsController.onPageLoad(
        id = None,
        mode = NormalMode,
        Some(SubmissionCyaViaAddedThirdParties)
      )
    }

    "must go to Third Party Org Details with MultipleThirdPartiesCya context when Yes and returnTo = MultipleThirdPartiesCya" in {

      val result =
        navigator.nextPageFromAddedThirdParties(
          answer = YesNoAnswer.Yes,
          count = 1,
          connectedOrganisations = Nil,
          mode = NormalMode,
          returnTo = Some(MultipleThirdPartiesCya)
        )

      result mustEqual ThirdPartyOrgDetailsController.onPageLoad(
        id = None,
        mode = NormalMode,
        Some(MultipleThirdPartiesCyaViaAddedThirdParties)
      )
    }

    "must go to Third Party Org Details with original returnTo when Yes and no special returnTo" in {

      val result =
        navigator.nextPageFromAddedThirdParties(
          answer = YesNoAnswer.Yes,
          count = 1,
          connectedOrganisations = Nil,
          mode = NormalMode,
          returnTo = None
        )

      result mustEqual ThirdPartyOrgDetailsController.onPageLoad(
        id = None,
        mode = NormalMode,
        None
      )
    }
  }
}
