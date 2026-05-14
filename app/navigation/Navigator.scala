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
import models.*
import models.ReturnTo.{SubmissionCya, ThirdPartyCya}
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

  def nextPageFromAddedLiaisonOfficers(
    answer: YesNoAnswer,
    mode: Mode,
    returnTo: Option[ReturnTo]
  ): Call =
    addedLiaisonOfficersNextPage(answer, returnTo)

  def nextPageFromAddedSignatories(
    answer: YesNoAnswer,
    mode: Mode,
    returnTo: Option[ReturnTo]
  ): Call =
    addedSignatoriesNextPage(answer, returnTo)

  def nextPageFromAddedThirdParties(
    answer: YesNoAnswer,
    count: Int,
    connectedOrganisations: Seq[String] = Nil,
    mode: Mode,
    returnTo: Option[ReturnTo]
  ): Call =
    addedThirdPartiesNextPage(answer, count, connectedOrganisations, mode, returnTo)

  // TODO: Consider creating navigator defs for each task list journey to keep maintainable and clear
  private[navigation] def normalRoutes[A <: TaskListSection](
    page: Page[A],
    answers: A,
    returnTo: Option[ReturnTo]
  ): Call = page match {
    case RegisteredIsaManagerPage                  => registeredIsaManagerNextPage(answers, returnTo)
    case ZReferenceNumberPage                      =>
      returnToRoute(returnTo, TradingUsingDifferentNameController.onPageLoad(NormalMode))
    case FirmReferenceNumberPage                   => RegisteredAddressCorrespondenceController.onPageLoad(NormalMode, returnTo)
    case TradingUsingDifferentNamePage             => tradingUsingDifferentNameNextPage(answers, returnTo)
    case TradingNamePage                           => returnToRoute(returnTo, FirmReferenceNumberController.onPageLoad(NormalMode))
    case RegisteredAddressCorrespondencePage       => registeredAddressCorrespondenceNextPage(answers, returnTo)
    case ChooseAddressPage                         => chooseAddressNextPage(answers, returnTo)
    case AddAnotherAddressPage                     => addAnotherAddressRouting(answers, returnTo)
    case EnterYourOrganisationAddressPage          =>
      ConfirmCorrespondenceAddressController.onPageLoad()
    case OrganisationTelephoneNumberPage           =>
      returnToRoute(
        returnTo,
        TaskListController.onPageLoad()
      ) // TODO make default call to OrganisationDetailsCYA when built in 1743
    case IsaProductsPage                           => isaProductsNextPage(answers, returnTo)
    case InnovativeFinancialProductsPage           => innovativeFinancialProductsNextPage(answers, returnTo)
    case PeerToPeerPlatformPage                    => PeerToPeerPlatformNumberController.onPageLoad(NormalMode, returnTo)
    case PeerToPeerPlatformNumberPage              => returnToRoute(returnTo, IsaProductsCheckYourAnswersController.onPageLoad())
    case CertificatesOfAuthorityYesNoPage          => certificatesOfAuthorityYesNoNextPage(answers, returnTo)
    case FcaArticlesPage                           => returnToRoute(returnTo, CoaCheckYourAnswersController.onPageLoad())
    case FinancialOrganisationPage                 => returnToRoute(returnTo, CoaCheckYourAnswersController.onPageLoad())
    case OrganisationEmailAddressPage              => EmailVerificationCodeController.onPageLoad(NormalMode, returnTo)
    case OrganisationEmailVerificationCodePage     => returnToRoute(returnTo, OrganisationEmailCyaController.onPageLoad())
    case LiaisonOfficerNamePage(id)                => LiaisonOfficerEmailController.onPageLoad(id, NormalMode, returnTo)
    case LiaisonOfficerEmailPage(id)               => LiaisonOfficerPhoneNumberController.onPageLoad(id, NormalMode, returnTo)
    case LiaisonOfficerPhoneNumberPage(id)         => LiaisonOfficerCommunicationController.onPageLoad(id, NormalMode, returnTo)
    case LiaisonOfficerCommunicationPage(id)       => LoCheckYourAnswersController.onPageLoad(id, returnTo)
    case RemoveLiaisonOfficerPage                  => removeLiaisonOfficerNextPage(answers, returnTo)
    case RemoveSignatoryPage(id)                   => removeSignatoryNextPage(answers, returnTo)
    case SignatoryNamePage(id)                     => SignatoryJobTitleController.onPageLoad(id = id, mode = NormalMode, returnTo)
    case SignatoryJobTitlePage(id)                 => SignatoryCheckYourAnswersController.onPageLoad(id = id, returnTo)
    case ProductsManagedByThirdPartyPage           => productsManagedByThirdPartyNextPage(answers, returnTo)
    case ThirdPartyOrgDetailsPage(id)              =>
      ThirdPartyManagingReturnsController.onPageLoad(id = id, mode = NormalMode, returnTo)
    case ThirdPartyManagingReturnsPage(id)         =>
      InvestorFundsUsedByThirdPartyController.onPageLoad(id = id, mode = NormalMode, returnTo)
    case InvestorFundsUsedByThirdPartyPage(id)     => investorFundsUsedByThirdPartyNextPage(answers, id, returnTo)
    case ThirdPartyInvestorFundsPercentagePage(id) =>
      thirdPartyCheckNextPage(id, returnTo)

    case RemoveThirdPartyPage =>
      removeThirdPartyNextPage(answers, returnTo)

    case ThirdPartyConnectedOrganisationsPage =>
      returnToRoute(returnTo, ThirdPartiesCheckYourAnswersController.onPageLoad())
    case _                                    => throw new NotImplementedError("No route for this page in normal mode")
  }

  private[navigation] def checkRouteMap[A <: TaskListSection](page: Page[A], returnTo: Option[ReturnTo]): Call =
    page match {
      case RegisteredIsaManagerPage =>
        returnToRoute(returnTo, TaskListController.onPageLoad())

      case ZReferenceNumberPage =>
        returnToRoute(returnTo, TaskListController.onPageLoad())

      case TradingUsingDifferentNamePage =>
        returnToRoute(returnTo, TaskListController.onPageLoad())

      case TradingNamePage =>
        returnToRoute(returnTo, TaskListController.onPageLoad())

      case FirmReferenceNumberPage =>
        returnToRoute(returnTo, TaskListController.onPageLoad())

      case RegisteredAddressCorrespondencePage =>
        returnToRoute(returnTo, TaskListController.onPageLoad())

      case ChooseAddressPage =>
        returnToRoute(returnTo, TaskListController.onPageLoad())

      case OrganisationTelephoneNumberPage =>
        returnToRoute(
          returnTo,
          TaskListController.onPageLoad()
        ) // TODO make default call to OrganisationDetailsCYA when built in 1743

      case EnterYourOrganisationAddressPage =>
        returnToRoute(returnTo, TaskListController.onPageLoad()) // TODO hook to review and confim page in DFI-934

      case OrganisationEmailAddressPage =>
        returnToRoute(returnTo, OrganisationEmailCyaController.onPageLoad())

      case OrganisationEmailVerificationCodePage =>
        returnToRoute(returnTo, OrganisationEmailCyaController.onPageLoad())

      case IsaProductsPage =>
        returnToRoute(returnTo, IsaProductsCheckYourAnswersController.onPageLoad())

      case InnovativeFinancialProductsPage =>
        returnToRoute(returnTo, IsaProductsCheckYourAnswersController.onPageLoad())

      case PeerToPeerPlatformPage =>
        returnToRoute(returnTo, IsaProductsCheckYourAnswersController.onPageLoad())

      case PeerToPeerPlatformNumberPage =>
        returnToRoute(returnTo, IsaProductsCheckYourAnswersController.onPageLoad())

      case CertificatesOfAuthorityYesNoPage =>
        returnToRoute(returnTo, CoaCheckYourAnswersController.onPageLoad())

      case FcaArticlesPage =>
        returnToRoute(returnTo, CoaCheckYourAnswersController.onPageLoad())

      case FinancialOrganisationPage =>
        returnToRoute(returnTo, CoaCheckYourAnswersController.onPageLoad())

      case LiaisonOfficerNamePage(id) =>
        liaisonOfficerCheckRoute(id, returnTo)

      case LiaisonOfficerEmailPage(id) =>
        liaisonOfficerCheckRoute(id, returnTo)

      case LiaisonOfficerPhoneNumberPage(id) =>
        liaisonOfficerCheckRoute(id, returnTo)

      case LiaisonOfficerCommunicationPage(id) =>
        liaisonOfficerCheckRoute(id, returnTo)

      case SignatoryNamePage(id) =>
        signatoryCheckRoute(id, returnTo)

      case SignatoryJobTitlePage(id) =>
        signatoryCheckRoute(id, returnTo)

      case ProductsManagedByThirdPartyPage =>
        returnToRoute(returnTo, ThirdPartiesCheckYourAnswersController.onPageLoad())

      case ThirdPartyOrgDetailsPage(id) =>
        thirdPartyCheckModeRoute(id, returnTo)

      case ThirdPartyManagingReturnsPage(id) =>
        thirdPartyCheckModeRoute(id, returnTo)

      case InvestorFundsUsedByThirdPartyPage(id) =>
        thirdPartyCheckModeRoute(id, returnTo)

      case ThirdPartyInvestorFundsPercentagePage(id) =>
        thirdPartyCheckModeRoute(id, returnTo)

      case ThirdPartyConnectedOrganisationsPage =>
        returnToRoute(returnTo, ThirdPartiesCheckYourAnswersController.onPageLoad())

      case _ =>
        throw new NotImplementedError("No route for this page in check mode")
    }

  private def registeredIsaManagerNextPage(answers: OrganisationDetails, returnTo: Option[ReturnTo]): Call =
    answers.registeredToManageIsa match {
      case Some(true) => ZReferenceNumberController.onPageLoad(NormalMode, returnTo)
      case _          => TradingUsingDifferentNameController.onPageLoad(NormalMode, returnTo)
    }

  private def tradingUsingDifferentNameNextPage(answers: OrganisationDetails, returnTo: Option[ReturnTo]): Call =
    answers.tradingUsingDifferentName.fold(TaskListController.onPageLoad()) {
      case true  => TradingNameController.onPageLoad(NormalMode, returnTo)
      case false => FirmReferenceNumberController.onPageLoad(NormalMode, returnTo)
    }

  private def isaProductsNextPage(answers: IsaProducts, returnTo: Option[ReturnTo]): Call =
    answers.isaProducts.fold(IndexController.onPageLoad()) { isaProducts =>
      if (isaProducts.contains(InnovativeFinanceIsas))
        InnovativeFinancialProductsController.onPageLoad(NormalMode, returnTo)
      else returnToRoute(returnTo, IsaProductsCheckYourAnswersController.onPageLoad())
    }

  private def innovativeFinancialProductsNextPage(answers: IsaProducts, returnTo: Option[ReturnTo]): Call =
    answers.innovativeFinancialProducts.fold(IndexController.onPageLoad()) { ifps =>
      if (ifps.contains(PeertopeerLoansUsingAPlatformWith36hPermissions))
        PeerToPeerPlatformController.onPageLoad(NormalMode, returnTo)
      else returnToRoute(returnTo, IsaProductsCheckYourAnswersController.onPageLoad())
    }

  private def certificatesOfAuthorityYesNoNextPage(
    answers: CertificatesOfAuthority,
    returnTo: Option[ReturnTo]
  ): Call =
    answers.certificatesYesNo.fold(CertificatesOfAuthorityYesNoController.onPageLoad(NormalMode, returnTo)) {
      case Yes =>
        FcaArticlesController.onPageLoad(NormalMode, returnTo)
      case No  =>
        FinancialOrganisationController.onPageLoad(NormalMode, returnTo)
    }

  private def registeredAddressCorrespondenceNextPage(
    answers: OrganisationDetails,
    returnTo: Option[ReturnTo]
  ): Call =
    answers.registeredAddressCorrespondence.fold(IndexController.onPageLoad()) {
      case true  =>
        OrganisationTelephoneNumberController.onPageLoad(NormalMode, returnTo)
      case false =>
        AddAnotherAddressController.onPageLoad(NormalMode, returnTo)
    }

  private def addedLiaisonOfficersNextPage(answer: YesNoAnswer, returnTo: Option[ReturnTo]) =
    answer match {
      case YesNoAnswer.Yes => LiaisonOfficerNameController.onPageLoad(None, NormalMode, returnTo)
      case YesNoAnswer.No  => returnToRoute(returnTo, TaskListController.onPageLoad())
    }

  private def addedSignatoriesNextPage(answer: YesNoAnswer, returnTo: Option[ReturnTo]) =
    answer match {
      case YesNoAnswer.Yes => SignatoryNameController.onPageLoad(None, NormalMode, returnTo)
      case YesNoAnswer.No  => returnToRoute(returnTo, TaskListController.onPageLoad())
    }

  private def addedThirdPartiesNextPage(
    answer: YesNoAnswer,
    count: Int,
    connectedOrganisations: Seq[String],
    mode: Mode,
    returnTo: Option[ReturnTo]
  ): Call =
    answer match {
      case YesNoAnswer.Yes => ThirdPartyOrgDetailsController.onPageLoad(id = None, mode = NormalMode, returnTo)
      case YesNoAnswer.No  =>
        if (count > 1 && connectedOrganisations.isEmpty)
          ThirdPartyConnectedOrganisationsController.onPageLoad(NormalMode, returnTo)
        else
          returnToRoute(returnTo, addedThirdPartiesDefaultNoNextPage(count, mode))
    }

  private def addedThirdPartiesDefaultNoNextPage(count: Int, mode: Mode): Call =
    if (count > 1 && mode == NormalMode) ThirdPartyConnectedOrganisationsController.onPageLoad(NormalMode, None)
    else if (count > 1) ThirdPartiesCheckYourAnswersController.onPageLoad()
    else TaskListController.onPageLoad()

  private def removeSignatoryNextPage(
    answers: Signatories,
    returnTo: Option[ReturnTo]
  ): Call =
    answers.signatories match {
      case Seq() => AddASignatoryController.onPageLoad(returnTo)
      case _     => AddedSignatoryController.onPageLoad(NormalMode, returnTo)
    }

  private def removeLiaisonOfficerNextPage(answers: LiaisonOfficers, returnTo: Option[ReturnTo]): Call =
    answers.liaisonOfficers match {
      case Nil => AddLiaisonOfficerController.onPageLoad(returnTo)
      case _   => AddedLiaisonOfficersController.onPageLoad(NormalMode, returnTo)
    }

  private def productsManagedByThirdPartyNextPage(answers: ThirdPartyOrganisations, returnTo: Option[ReturnTo]): Call =
    answers.managedByThirdParty match {
      case Some(YesNoAnswer.Yes) => ThirdPartyOrgDetailsController.onPageLoad(id = None, mode = NormalMode, returnTo)
      case _                     => returnToRoute(returnTo, TaskListController.onPageLoad())
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
        ThirdPartyCheckYourAnswersController.onPageLoad(id = id, returnTo)
      case _                     =>
        TaskListController.onPageLoad()
    }

  private def removeThirdPartyNextPage(answers: ThirdPartyOrganisations, returnTo: Option[ReturnTo]): Call =
    answers.thirdParties match {
      case Nil => ProductsManagedByThirdPartyController.onPageLoad(NormalMode, returnTo)
      case _   => AddedThirdPartiesController.onPageLoad(NormalMode, returnTo)
    }

  private def addAnotherAddressRouting(answers: OrganisationDetails, returnTo: Option[ReturnTo]): Call = {

    val count =
      answers.addAnotherAddress
        .map(_.addresses.size)
        .getOrElse(0)

    count match {
      case 1          =>
        ConfirmCorrespondenceAddressController.onPageLoad()
      case n if n > 1 =>
        ChooseAddressController.onPageLoad(NormalMode, returnTo)
      case _          =>
        TaskListController.onPageLoad()
    }
  }

  private def thirdPartyCheckNextPage(
    id: String,
    returnTo: Option[ReturnTo]
  ): Call =
    returnTo match {
      case Some(ThirdPartyCya) =>
        ThirdPartiesCheckYourAnswersController.onPageLoad()
      case _                   =>
        ThirdPartyCheckYourAnswersController.onPageLoad(id, returnTo)
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
        ConfirmCorrespondenceAddressController.onPageLoad()
      case None                                            =>
        TaskListController.onPageLoad()
    }

  private def liaisonOfficerCheckRoute(id: String, returnTo: Option[ReturnTo]): Call =
    returnToRoute(
      returnTo,
      LoCheckYourAnswersController.onPageLoad(id, None)
    )

  private def signatoryCheckRoute(id: String, returnTo: Option[ReturnTo]): Call =
    returnToRoute(
      returnTo,
      SignatoryCheckYourAnswersController.onPageLoad(id = id, returnTo = None)
    )

  private def thirdPartyCheckModeRoute(id: String, returnTo: Option[ReturnTo]): Call =
    returnToRoute(
      returnTo,
      ThirdPartyCheckYourAnswersController.onPageLoad(id = id, returnTo = None)
    )

  private def returnToRoute(returnTo: Option[ReturnTo], default: => Call): Call =
    returnTo match {
      case Some(SubmissionCya) => SubmissionCyaController.onPageLoad()
      case Some(ThirdPartyCya) => ThirdPartiesCheckYourAnswersController.onPageLoad()
      case _                   => default
    }
}
