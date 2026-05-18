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

import controllers.certificatesofauthority.routes.*
import controllers.isaproducts.routes.*
import controllers.liaisonofficers.routes.*
import controllers.orgdetails.routes.*
import controllers.orgemail.routes.*
import controllers.routes.*
import controllers.signatories.routes.*
import controllers.thirdparty.routes.*
import models.{YesNoAnswer, *}
import models.ReturnTo.{FinalCya, OrganisationDetailsCya}
import models.journeydata.certificatesofauthority.CertificatesOfAuthority
import models.journeydata.certificatesofauthority.CertificatesOfAuthorityYesNo.*
import models.journeydata.isaproducts.InnovativeFinancialProduct.PeertopeerLoansUsingAPlatformWith36hPermissions
import models.journeydata.isaproducts.IsaProduct.InnovativeFinanceIsas
import models.journeydata.isaproducts.IsaProducts
import models.journeydata.liaisonofficers.LiaisonOfficers
import models.journeydata.orgdetails.SelectedCorrespondenceAddress
import models.journeydata.signatories.Signatories
import models.journeydata.thirdparty.ThirdPartyOrganisations
import models.journeydata.{OrganisationDetails, TaskListSection}
import pages.*
import pages.certificatesofauthority.{CertificatesOfAuthorityYesNoPage, FcaArticlesPage, FinancialOrganisationPage}
import pages.isaproducts.{InnovativeFinancialProductsPage, IsaProductsPage, PeerToPeerPlatformNumberPage, PeerToPeerPlatformPage}
import pages.liaisonofficers.*
import pages.organisationdetails.*
import pages.orgemail.*
import pages.signatories.{RemoveSignatoryPage, SignatoryJobTitlePage, SignatoryNamePage}
import pages.thirdparty.*
import play.api.mvc.Call

import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject() () {

  def nextPage[A <: TaskListSection](
    page: PageWithDependents[A],
    existing: Option[A],
    updated: A,
    mode: Mode,
    returnTo: Option[ReturnTo]
  ): Call = {
    val onwardMode: Mode =
      existing.fold(NormalMode)(existing => if (page.resumeNormalMode(updated)) NormalMode else mode)

    onwardMode match {
      case NormalMode =>
        normalRoutes(page, updated, returnTo)
      case CheckMode  =>
        checkRouteMap(page, returnTo)
    }
  }

  def nextPage[A <: TaskListSection](
    page: Page[A],
    updated: A,
    mode: Mode,
    returnTo: Option[ReturnTo]
  ): Call =
    mode match {
      case NormalMode =>
        normalRoutes(page, updated, returnTo)
      case CheckMode  =>
        checkRouteMap(page, returnTo)
    }

  // TODO: Consider creating navigator defs for each task list journey to keep maintainable and clear
  private[navigation] def normalRoutes[A <: TaskListSection](
    page: Page[A],
    answers: A,
    returnTo: Option[ReturnTo]
  ): Call = page match {
    case RegisteredIsaManagerPage                  => registeredIsaManagerPageNextPage(answers, returnTo)
    case ZReferenceNumberPage                      => zReferencePageNextPage(returnTo)
    case FirmReferenceNumberPage                   => RegisteredAddressCorrespondenceController.onPageLoad(NormalMode, None)
    case TradingUsingDifferentNamePage             => tradingUsingDifferentNameNextPage(answers, returnTo)
    case TradingNamePage                           => tradingNameNextPage(returnTo)
    case IsaProductsPage                           => isaProductsNextPage(answers)
    case InnovativeFinancialProductsPage           => innovativeFinancialProductsNextPage(answers)
    case PeerToPeerPlatformPage                    => PeerToPeerPlatformNumberController.onPageLoad(NormalMode)
    case PeerToPeerPlatformNumberPage              => IsaProductsCheckYourAnswersController.onPageLoad()
    case CertificatesOfAuthorityYesNoPage          => certificatesOfAuthorityYesNoNextPage(answers)
    case FcaArticlesPage                           => CoaCheckYourAnswersController.onPageLoad()
    case FinancialOrganisationPage                 => CoaCheckYourAnswersController.onPageLoad()
    case RegisteredAddressCorrespondencePage       => registeredAddressCorrespondenceNextPage(answers, returnTo)
    case ChooseAddressPage                         => chooseAddressNextPage(answers, returnTo)
    case AddAnotherAddressPage                     => addAnotherAddressRouting(answers, returnTo)
    case EnterYourOrganisationAddressPage          => ConfirmCorrespondenceAddressController.onPageLoad(returnTo)
    case ConfirmAddressPage                        => confirmAddressRouting(returnTo)
    case OrganisationTelephoneNumberPage           => OrganisationDetailsCheckYourAnswersController.onPageLoad()
    case OrganisationEmailAddressPage              => EmailVerificationCodeController.onPageLoad()
    case OrganisationEmailVerificationCodePage     => OrganisationEmailCyaController.onPageLoad()
    case LiaisonOfficerNamePage(id)                => LiaisonOfficerEmailController.onPageLoad(id, NormalMode)
    case LiaisonOfficerEmailPage(id)               => LiaisonOfficerPhoneNumberController.onPageLoad(id, NormalMode)
    case LiaisonOfficerPhoneNumberPage(id)         => LiaisonOfficerCommunicationController.onPageLoad(id, NormalMode)
    case LiaisonOfficerCommunicationPage(id)       => LoCheckYourAnswersController.onPageLoad(id)
    case RemoveLiaisonOfficerPage                  => removeLiaisonOfficerNextPage(answers)
    case RemoveSignatoryPage(id)                   => removeSignatoryNextPage(answers)
    case SignatoryNamePage(id)                     => SignatoryJobTitleController.onPageLoad(id = id, mode = NormalMode)
    case SignatoryJobTitlePage(id)                 => SignatoryCheckYourAnswersController.onPageLoad(id = id)
    case ProductsManagedByThirdPartyPage           => productsManagedByThirdPartNextPage(answers)
    case ThirdPartyOrgDetailsPage(id)              =>
      ThirdPartyManagingReturnsController.onPageLoad(id = id, mode = NormalMode, None)
    case ThirdPartyManagingReturnsPage(id)         =>
      InvestorFundsUsedByThirdPartyController.onPageLoad(id = id, mode = NormalMode, None)
    case InvestorFundsUsedByThirdPartyPage(id)     => investorFundsUsedByThirdPartyNextPage(answers, id, returnTo)
    case ThirdPartyInvestorFundsPercentagePage(id) => thirdPartyCheckNextPage(id, returnTo)
    case RemoveThirdPartyPage                      => removeThirdPartyNextPage(answers)
    case ThirdPartyConnectedOrganisationsPage      => ThirdPartiesCheckYourAnswersController.onPageLoad()
    case _                                         => throw new NotImplementedError("No route for this page in normal mode")
  }

  private[navigation] def checkRouteMap[A <: TaskListSection](page: Page[A], returnTo: Option[ReturnTo]): Call =
    page match {
      case RegisteredIsaManagerPage                  => OrganisationDetailsCheckYourAnswersController.onPageLoad()
      case ZReferenceNumberPage                      => OrganisationDetailsCheckYourAnswersController.onPageLoad()
      case TradingUsingDifferentNamePage             => OrganisationDetailsCheckYourAnswersController.onPageLoad()
      case TradingNamePage                           => OrganisationDetailsCheckYourAnswersController.onPageLoad()
      case FirmReferenceNumberPage                   => OrganisationDetailsCheckYourAnswersController.onPageLoad()
      case OrganisationTelephoneNumberPage           => OrganisationDetailsCheckYourAnswersController.onPageLoad()
      case IsaProductsPage                           => IsaProductsCheckYourAnswersController.onPageLoad()
      case InnovativeFinancialProductsPage           => IsaProductsCheckYourAnswersController.onPageLoad()
      case PeerToPeerPlatformPage                    => IsaProductsCheckYourAnswersController.onPageLoad()
      case PeerToPeerPlatformNumberPage              => IsaProductsCheckYourAnswersController.onPageLoad()
      case CertificatesOfAuthorityYesNoPage          => CoaCheckYourAnswersController.onPageLoad()
      case FcaArticlesPage                           => CoaCheckYourAnswersController.onPageLoad()
      case FinancialOrganisationPage                 => CoaCheckYourAnswersController.onPageLoad()
      case RegisteredAddressCorrespondencePage       => OrganisationDetailsCheckYourAnswersController.onPageLoad()
      case EnterYourOrganisationAddressPage          => OrganisationDetailsCheckYourAnswersController.onPageLoad()
      case OrganisationEmailAddressPage              => OrganisationEmailCyaController.onPageLoad()
      case LiaisonOfficerNamePage(id)                => LoCheckYourAnswersController.onPageLoad(id)
      case LiaisonOfficerEmailPage(id)               => LoCheckYourAnswersController.onPageLoad(id)
      case LiaisonOfficerPhoneNumberPage(id)         => LoCheckYourAnswersController.onPageLoad(id)
      case LiaisonOfficerCommunicationPage(id)       => LoCheckYourAnswersController.onPageLoad(id)
      case SignatoryNamePage(id)                     => SignatoryCheckYourAnswersController.onPageLoad(id = id)
      case SignatoryJobTitlePage(id)                 => SignatoryCheckYourAnswersController.onPageLoad(id = id)
      case ProductsManagedByThirdPartyPage           => ThirdPartiesCheckYourAnswersController.onPageLoad()
      case ThirdPartyOrgDetailsPage(id)              => thirdPartyCheckNextPage(id, returnTo)
      case ThirdPartyManagingReturnsPage(id)         => thirdPartyCheckNextPage(id, returnTo)
      case InvestorFundsUsedByThirdPartyPage(id)     => thirdPartyCheckNextPage(id, returnTo)
      case ThirdPartyInvestorFundsPercentagePage(id) => thirdPartyCheckNextPage(id, returnTo)
      case ThirdPartyConnectedOrganisationsPage      => ThirdPartiesCheckYourAnswersController.onPageLoad()
      case _                                         => throw new NotImplementedError("No route for this page in check mode")
    }

  private def registeredIsaManagerPageNextPage(
    answers: OrganisationDetails,
    returnTo: Option[ReturnTo]
  ): Call =
    (answers.registeredToManageIsa, returnTo) match {
      case (Some(YesNoAnswer.Yes), Some(ReturnTo.OrganisationDetailsCya)) =>
        ZReferenceNumberController.onPageLoad(NormalMode, returnTo)
      case (Some(YesNoAnswer.No), Some(ReturnTo.OrganisationDetailsCya))  =>
        OrganisationDetailsCheckYourAnswersController.onPageLoad()
      case (Some(YesNoAnswer.Yes), _)                                     =>
        ZReferenceNumberController.onPageLoad(NormalMode, returnTo)
      case (Some(YesNoAnswer.No), _)                                      =>
        TradingUsingDifferentNameController.onPageLoad(NormalMode, None)
      case _                                                              =>
        TaskListController.onPageLoad()
    }

  private def zReferencePageNextPage(returnTo: Option[ReturnTo]): Call =
    returnTo match {
      case Some(ReturnTo.OrganisationDetailsCya) =>
        OrganisationDetailsCheckYourAnswersController.onPageLoad()
      case _                                     =>
        TradingUsingDifferentNameController.onPageLoad(NormalMode, None)
    }

  private def tradingUsingDifferentNameNextPage(
    answers: OrganisationDetails,
    returnTo: Option[ReturnTo]
  ): Call =
    (answers.tradingUsingDifferentName, returnTo) match {
      case (Some(YesNoAnswer.Yes), Some(ReturnTo.OrganisationDetailsCya)) =>
        TradingNameController.onPageLoad(NormalMode, returnTo)
      case (Some(YesNoAnswer.No), Some(ReturnTo.OrganisationDetailsCya))  =>
        OrganisationDetailsCheckYourAnswersController.onPageLoad()
      case (Some(YesNoAnswer.Yes), _)                                     =>
        TradingNameController.onPageLoad(NormalMode, returnTo)
      case (Some(YesNoAnswer.No), _)                                      =>
        FirmReferenceNumberController.onPageLoad(NormalMode)
      case _                                                              =>
        TaskListController.onPageLoad()
    }

  private def tradingNameNextPage(returnTo: Option[ReturnTo]): Call =
    returnTo match {
      case Some(OrganisationDetailsCya) => OrganisationDetailsCheckYourAnswersController.onPageLoad()
      case _                            => FirmReferenceNumberController.onPageLoad(NormalMode)
    }

  private def isaProductsNextPage(answers: IsaProducts): Call =
    answers.isaProducts.fold(IndexController.onPageLoad()) { isaProducts =>
      if (isaProducts.contains(InnovativeFinanceIsas)) InnovativeFinancialProductsController.onPageLoad(NormalMode)
      else IsaProductsCheckYourAnswersController.onPageLoad()
    }

  private def innovativeFinancialProductsNextPage(answers: IsaProducts): Call =
    answers.innovativeFinancialProducts.fold(IndexController.onPageLoad()) { ifps =>
      if (ifps.contains(PeertopeerLoansUsingAPlatformWith36hPermissions))
        PeerToPeerPlatformController.onPageLoad(NormalMode)
      else IsaProductsCheckYourAnswersController.onPageLoad()
    }

  private def certificatesOfAuthorityYesNoNextPage(
    answers: CertificatesOfAuthority
  ): Call =
    answers.certificatesYesNo.fold(CertificatesOfAuthorityYesNoController.onPageLoad(NormalMode)) {
      case Yes =>
        FcaArticlesController.onPageLoad(NormalMode)
      case No  =>
        FinancialOrganisationController.onPageLoad(NormalMode)
    }

  private def registeredAddressCorrespondenceNextPage(
    answers: OrganisationDetails,
    returnTo: Option[ReturnTo]
  ): Call =
    answers.registeredAddressCorrespondence.fold(IndexController.onPageLoad()) {
      case YesNoAnswer.Yes =>
        OrganisationTelephoneNumberController.onPageLoad(NormalMode)
      case YesNoAnswer.No  =>
        AddAnotherAddressController.onPageLoad(NormalMode, returnTo)
    }

  private def removeSignatoryNextPage(
    answers: Signatories
  ): Call =
    answers.signatories match {
      case Seq() => AddASignatoryController.onPageLoad()
      case _     => AddedSignatoryController.onPageLoad()
    }

  private def removeLiaisonOfficerNextPage(answers: LiaisonOfficers): Call =
    answers.liaisonOfficers match {
      case Nil => AddLiaisonOfficerController.onPageLoad()
      case _   => AddedLiaisonOfficersController.onPageLoad()
    }

  private def productsManagedByThirdPartNextPage(answers: ThirdPartyOrganisations): Call =
    answers.managedByThirdParty match {
      case Some(YesNoAnswer.Yes) => ThirdPartyOrgDetailsController.onPageLoad(id = None, mode = NormalMode, None)
      case _                     => TaskListController.onPageLoad()
    }

  private def investorFundsUsedByThirdPartyNextPage(
    answers: ThirdPartyOrganisations,
    id: String,
    returnTo: Option[ReturnTo]
  ): Call =
    answers.thirdParties
      .find(_.id == id)
      .flatMap(_.usingInvestorFunds) match {
      case Some(YesNoAnswer.Yes) =>
        ThirdPartyInvestorFundsPercentageController.onPageLoad(id = id, mode = NormalMode, returnTo)
      case Some(YesNoAnswer.No)  =>
        ThirdPartyCheckYourAnswersController.onPageLoad(id = id)
      case _                     =>
        TaskListController.onPageLoad()
    }

  private def removeThirdPartyNextPage(answers: ThirdPartyOrganisations): Call =
    answers.thirdParties match {
      case Nil => ProductsManagedByThirdPartyController.onPageLoad(NormalMode)
      case _   => AddedThirdPartiesController.onPageLoad(NormalMode, None)
    }

  private def addAnotherAddressRouting(answers: OrganisationDetails, returnTo: Option[ReturnTo]): Call = {
    val count =
      answers.addAnotherAddress
        .map(_.addresses.size)
        .getOrElse(0)

    count match {
      case 1          =>
        ConfirmCorrespondenceAddressController.onPageLoad(returnTo)
      case n if n > 1 =>
        ChooseAddressController.onPageLoad(NormalMode, returnTo)
      case _          =>
        TaskListController.onPageLoad()
    }
  }

  private def confirmAddressRouting(returnTo: Option[ReturnTo]): Call =
    returnTo match {
      case Some(ReturnTo.OrganisationDetailsCya) =>
        OrganisationDetailsCheckYourAnswersController.onPageLoad()
      case _                                     =>
        OrganisationTelephoneNumberController.onPageLoad(NormalMode)
    }

  private def thirdPartyCheckNextPage(
    id: String,
    returnTo: Option[ReturnTo]
  ): Call =
    returnTo match {
      case Some(FinalCya) =>
        ThirdPartiesCheckYourAnswersController.onPageLoad()
      case _              =>
        ThirdPartyCheckYourAnswersController.onPageLoad(id)
    }

  private def chooseAddressNextPage(
    answers: OrganisationDetails,
    returnTo: Option[ReturnTo]
  ): Call =
    answers.addAnotherAddress
      .flatMap(_.selectedAddress) match {
      case Some(SelectedCorrespondenceAddress.ManualEntry) =>
        EnterYourOrganisationAddressController.onPageLoad(NormalMode, returnTo)
      case Some(SelectedCorrespondenceAddress.Address(_))  =>
        ConfirmCorrespondenceAddressController.onPageLoad(returnTo)
      case None                                            =>
        TaskListController.onPageLoad()
    }
}
