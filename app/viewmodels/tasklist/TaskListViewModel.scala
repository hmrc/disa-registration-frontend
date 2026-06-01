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

package viewmodels.tasklist

import controllers.certificatesofauthority.routes.{CoaCheckYourAnswersController, EligibilityToManageIsaController}
import controllers.isaproducts.routes.{IsaProductsCheckYourAnswersController, IsaProductsController}
import controllers.liaisonofficers.routes.{AddedLiaisonOfficersController, LiaisonOfficerNameController}
import controllers.orgdetails.routes.{OrganisationDetailsCheckYourAnswersController, RegisteredIsaManagerController}
import controllers.orgemail.routes.{EmailVerificationCodeController, OrganisationEmailAddressController, OrganisationEmailCyaController}
import controllers.signatories.routes.{AddedSignatoryController, SignatoryNameController}
import controllers.thirdparty.routes.{AddedThirdPartiesController, ProductsManagedByThirdPartyController, ThirdPartiesCheckYourAnswersController}
import controllers.routes.SubmissionCyaController
import models.journeydata.certificatesofauthority.CertificatesOfAuthorityYesNo.{No as CertificatesNo, Yes as CertificatesYes}
import models.journeydata.isaproducts.InnovativeFinancialProduct.PeertopeerLoansUsingAPlatformWith36hPermissions
import models.journeydata.isaproducts.IsaProduct.InnovativeFinanceIsas
import models.journeydata.JourneyData
import models.{CheckMode, NormalMode, YesNoAnswer}
import play.api.i18n.Messages
import uk.gov.hmrc.auth.core.{CredentialRole, User}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{Empty, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.tag.Tag
import uk.gov.hmrc.govukfrontend.views.viewmodels.tasklist.{TaskList, TaskListItem, TaskListItemStatus, TaskListItemTitle}

case class TaskListStatus(content: String, tagClass: Option[String] = None) {
  def toGovuk: TaskListItemStatus =
    if (this == TaskListStatus.NoStatus) {
      TaskListItemStatus()
    } else {
      TaskListItemStatus(
        tag = tagClass.map(cssClass => Tag(content = Text(content), classes = cssClass)),
        content = if (tagClass.isDefined) Empty else Text(content)
      )
    }
}

object TaskListStatus {
  val NoStatus: TaskListStatus = TaskListStatus("")
}

case class TaskListTaskViewModel(
  title: String,
  href: Option[String],
  status: TaskListStatus
) {
  def toGovuk: TaskListItem =
    TaskListItem(
      title = TaskListItemTitle(content = Text(title)),
      href = href,
      status = status.toGovuk
    )
}

case class TaskListSectionViewModel(
  heading: String,
  tasks: Seq[TaskListTaskViewModel]
) {
  def toGovuk: TaskList =
    TaskList(items = tasks.map(_.toGovuk))
}

case class TaskListViewModel(
  sections: Seq[TaskListSectionViewModel],
  canSubmitAnswers: Boolean
)

object TaskListViewModel {

  def apply(journeyData: JourneyData, credentialRole: CredentialRole)(implicit
    messages: Messages
  ): TaskListViewModel = {
    val remainingTasksUnlocked = canAccessTaskList(journeyData)
    val submitAnswersAvailable = canSubmitAnswers(journeyData, credentialRole)

    TaskListViewModel(
      sections = Seq(
        TaskListSectionViewModel(
          messages("taskList.section.organisationInformation"),
          Seq(
            organisationInformationTask(journeyData),
            organisationEmailTask(journeyData, remainingTasksUnlocked)
          )
        ),
        TaskListSectionViewModel(
          messages("taskList.section.eligibility"),
          Seq(
            isaProductsTask(journeyData, remainingTasksUnlocked),
            certificatesOfAuthorityTask(journeyData, remainingTasksUnlocked)
          )
        ),
        TaskListSectionViewModel(
          messages("taskList.section.authorisedUsers"),
          Seq(
            liaisonOfficersTask(journeyData, remainingTasksUnlocked),
            signatoriesTask(journeyData, remainingTasksUnlocked)
          )
        ),
        TaskListSectionViewModel(
          messages("taskList.section.thirdPartyOrganisations"),
          Seq(thirdPartyOrganisationsTask(journeyData, remainingTasksUnlocked))
        ),
        TaskListSectionViewModel(
          messages("taskList.section.submit"),
          Seq(submitAnswersTask(submitAnswersAvailable, credentialRole))
        )
      ),
      canSubmitAnswers = submitAnswersAvailable
    )
  }

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

  private def organisationInformationTask(journeyData: JourneyData)(implicit
    messages: Messages
  ): TaskListTaskViewModel = {
    val completed = isOrganisationInformationComplete(journeyData)
    val started   = organisationInformationStarted(journeyData)

    TaskListTaskViewModel(
      title =
        if (completed) messages("taskList.organisationInformation.change")
        else messages("taskList.organisationInformation.add"),
      href = Some(
        if (completed) OrganisationDetailsCheckYourAnswersController.onPageLoad().url
        else RegisteredIsaManagerController.onPageLoad(NormalMode).url
      ),
      status =
        if (completed) plain("taskList.status.completed")
        else if (started) blueTag("taskList.status.inProgress")
        else blueTag("taskList.status.notYetStarted")
    )
  }

  private def organisationEmailTask(journeyData: JourneyData, unlocked: Boolean)(implicit
    messages: Messages
  ): TaskListTaskViewModel = {
    val email    = journeyData.organisationEmail.flatMap(_.organisationEmail)
    val verified = isOrganisationEmailVerified(journeyData)

    TaskListTaskViewModel(
      title =
        if (verified) messages("taskList.organisationEmail.change")
        else messages("taskList.organisationEmail.add"),
      href = hrefWhen(
        unlocked,
        if (verified) OrganisationEmailCyaController.onPageLoad().url
        else if (email.isDefined) EmailVerificationCodeController.onPageLoad(NormalMode).url
        else OrganisationEmailAddressController.onPageLoad(NormalMode).url
      ),
      status =
        if (!unlocked) plain("taskList.status.cannotStartYet")
        else if (verified) plain("taskList.status.verified")
        else if (email.isDefined) yellowTag("taskList.status.notVerified")
        else blueTag("taskList.status.notYetStarted")
    )
  }

  private def isaProductsTask(journeyData: JourneyData, unlocked: Boolean)(implicit
    messages: Messages
  ): TaskListTaskViewModel =
    standardTask(
      unlocked = unlocked,
      complete = isIsaProductsComplete(journeyData),
      started = isaProductsStarted(journeyData),
      addMessage = "taskList.isaProducts.add",
      changeMessage = "taskList.isaProducts.change",
      addHref = IsaProductsController.onPageLoad(NormalMode).url,
      changeHref = IsaProductsCheckYourAnswersController.onPageLoad().url
    )

  private def certificatesOfAuthorityTask(journeyData: JourneyData, unlocked: Boolean)(implicit
    messages: Messages
  ): TaskListTaskViewModel =
    standardTask(
      unlocked = unlocked,
      complete = isCertificatesOfAuthorityComplete(journeyData),
      started = certificatesOfAuthorityStarted(journeyData),
      addMessage = "taskList.certificatesOfAuthority.add",
      changeMessage = "taskList.certificatesOfAuthority.change",
      addHref = EligibilityToManageIsaController.onPageLoad().url,
      changeHref = CoaCheckYourAnswersController.onPageLoad().url
    )

  private def liaisonOfficersTask(journeyData: JourneyData, unlocked: Boolean)(implicit
    messages: Messages
  ): TaskListTaskViewModel = {
    val liaisonOfficers = journeyData.liaisonOfficers.fold(Seq.empty)(_.liaisonOfficers)

    countedTask(
      unlocked = unlocked,
      count = liaisonOfficers.size,
      hasIncompleteItems = liaisonOfficers.exists(_.inProgress),
      addMessage = "taskList.liaisonOfficers.add",
      changeMessage = "taskList.liaisonOfficers.change",
      addHref = LiaisonOfficerNameController.onPageLoad(id = None, mode = NormalMode, returnTo = None).url,
      changeHref = AddedLiaisonOfficersController.onPageLoad(NormalMode).url,
      singularMessage = "taskList.count.liaisonOfficer.one",
      pluralMessage = "taskList.count.liaisonOfficer.other"
    )
  }

  private def signatoriesTask(journeyData: JourneyData, unlocked: Boolean)(implicit
    messages: Messages
  ): TaskListTaskViewModel = {
    val signatories = journeyData.signatories.fold(Seq.empty)(_.signatories)

    countedTask(
      unlocked = unlocked,
      count = signatories.size,
      hasIncompleteItems = signatories.exists(_.inProgress),
      addMessage = "taskList.signatories.add",
      changeMessage = "taskList.signatories.change",
      addHref = SignatoryNameController.onPageLoad(id = None, mode = NormalMode, returnTo = None).url,
      changeHref = AddedSignatoryController.onPageLoad(NormalMode).url,
      singularMessage = "taskList.count.signatory.one",
      pluralMessage = "taskList.count.signatory.other"
    )
  }

  private def thirdPartyOrganisationsTask(journeyData: JourneyData, unlocked: Boolean)(implicit
    messages: Messages
  ): TaskListTaskViewModel = {
    val section            = journeyData.thirdPartyOrganisations
    val count              = section.fold(0)(_.thirdParties.size)
    val complete           = areThirdPartyOrganisationsComplete(journeyData)
    val started            = section.flatMap(_.managedByThirdParty).isDefined
    val hasIncompleteItems =
      section.exists(_.thirdParties.exists(_.inProgress))

    TaskListTaskViewModel(
      title =
        if (complete || count > 0) messages("taskList.thirdPartyOrganisations.change")
        else messages("taskList.thirdPartyOrganisations.add"),
      href = hrefWhen(
        unlocked,
        if (hasIncompleteItems) AddedThirdPartiesController.onPageLoad(NormalMode).url
        else if (count > 1) ThirdPartiesCheckYourAnswersController.onPageLoad().url
        else if (count > 0) AddedThirdPartiesController.onPageLoad(NormalMode).url
        else if (complete) ProductsManagedByThirdPartyController.onPageLoad(CheckMode).url
        else ProductsManagedByThirdPartyController.onPageLoad(NormalMode).url
      ),
      status =
        if (!unlocked) plain("taskList.status.cannotStartYet")
        else if (complete && count == 0) plain("taskList.status.completed")
        else if (hasIncompleteItems) blueTag("taskList.status.inProgress")
        else if (count > 0) countStatus(count, "taskList.count.thirdParty.one", "taskList.count.thirdParty.other")
        else if (started) blueTag("taskList.status.inProgress")
        else blueTag("taskList.status.notYetStarted")
    )
  }

  private def submitAnswersTask(canSubmit: Boolean, credentialRole: CredentialRole)(implicit
    messages: Messages
  ): TaskListTaskViewModel = {
    val isAdministrator = credentialRole == User

    TaskListTaskViewModel(
      title =
        if (isAdministrator) messages("taskList.submit.checkAnswers")
        else messages("taskList.submit.assistantCannotSubmit"),
      href = hrefWhen(canSubmit, SubmissionCyaController.onPageLoad().url),
      status =
        if (!isAdministrator) TaskListStatus.NoStatus
        else if (canSubmit) blueTag("taskList.status.notYetStarted")
        else plain("taskList.status.cannotStartYet")
    )
  }

  private def standardTask(
    unlocked: Boolean,
    complete: Boolean,
    started: Boolean,
    addMessage: String,
    changeMessage: String,
    addHref: String,
    changeHref: String
  )(implicit messages: Messages): TaskListTaskViewModel =
    TaskListTaskViewModel(
      title = if (complete) messages(changeMessage) else messages(addMessage),
      href = hrefWhen(unlocked, if (complete) changeHref else addHref),
      status =
        if (!unlocked) plain("taskList.status.cannotStartYet")
        else if (complete) plain("taskList.status.completed")
        else if (started) blueTag("taskList.status.inProgress")
        else blueTag("taskList.status.notYetStarted")
    )

  private def countedTask(
    unlocked: Boolean,
    count: Int,
    hasIncompleteItems: Boolean,
    addMessage: String,
    changeMessage: String,
    addHref: String,
    changeHref: String,
    singularMessage: String,
    pluralMessage: String
  )(implicit messages: Messages): TaskListTaskViewModel =
    TaskListTaskViewModel(
      title = if (count > 0) messages(changeMessage) else messages(addMessage),
      href = hrefWhen(unlocked, if (count > 0) changeHref else addHref),
      status =
        if (!unlocked) plain("taskList.status.cannotStartYet")
        else if (hasIncompleteItems) blueTag("taskList.status.inProgress")
        else if (count > 0) countStatus(count, singularMessage, pluralMessage)
        else blueTag("taskList.status.notYetStarted")
    )

  private def hrefWhen(enabled: Boolean, href: String): Option[String] =
    Option.when(enabled)(href)

  private def plain(message: String)(implicit messages: Messages): TaskListStatus =
    TaskListStatus(messages(message))

  private def blueTag(message: String)(implicit messages: Messages): TaskListStatus =
    TaskListStatus(messages(message), Some("govuk-tag--blue"))

  private def yellowTag(message: String)(implicit messages: Messages): TaskListStatus =
    TaskListStatus(messages(message), Some("govuk-tag--yellow"))

  private def countStatus(count: Int, singularMessage: String, pluralMessage: String)(implicit
    messages: Messages
  ): TaskListStatus =
    TaskListStatus(messages(if (count == 1) singularMessage else pluralMessage, count))

  private def isOrganisationInformationComplete(journeyData: JourneyData): Boolean =
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

  private def organisationInformationStarted(journeyData: JourneyData): Boolean =
    journeyData.organisationDetails.exists { organisationDetails =>
      Seq(
        organisationDetails.registeredToManageIsa,
        organisationDetails.zRefNumber,
        organisationDetails.tradingUsingDifferentName,
        organisationDetails.tradingName,
        organisationDetails.fcaNumber,
        organisationDetails.registeredAddressCorrespondence,
        organisationDetails.correspondenceAddress,
        organisationDetails.orgTelephoneNumber,
        organisationDetails.addAnotherAddress
      ).exists(_.isDefined)
    }

  private def isOrganisationEmailVerified(journeyData: JourneyData): Boolean =
    journeyData.organisationEmail.exists { organisationEmail =>
      organisationEmail.organisationEmail.exists(nonEmpty) && organisationEmail.verified.contains(true)
    }

  private def isIsaProductsComplete(journeyData: JourneyData): Boolean =
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

  private def isaProductsStarted(journeyData: JourneyData): Boolean =
    journeyData.isaProducts.exists { isaProducts =>
      Seq(
        isaProducts.isaProducts,
        isaProducts.innovativeFinancialProducts,
        isaProducts.p2pPlatform,
        isaProducts.p2pPlatformNumber
      ).exists(_.isDefined)
    }

  private def isCertificatesOfAuthorityComplete(journeyData: JourneyData): Boolean =
    journeyData.certificatesOfAuthority.exists { certificatesOfAuthority =>
      certificatesOfAuthority.certificatesYesNo.exists {
        case CertificatesYes => certificatesOfAuthority.fcaArticles.exists(_.nonEmpty)
        case CertificatesNo  => certificatesOfAuthority.financialOrganisation.exists(_.nonEmpty)
      }
    }

  private def certificatesOfAuthorityStarted(journeyData: JourneyData): Boolean =
    journeyData.certificatesOfAuthority.exists { certificatesOfAuthority =>
      Seq(
        certificatesOfAuthority.certificatesYesNo,
        certificatesOfAuthority.fcaArticles,
        certificatesOfAuthority.financialOrganisation
      ).exists(_.isDefined)
    }

  private def areLiaisonOfficersComplete(journeyData: JourneyData): Boolean =
    journeyData.liaisonOfficers.exists { liaisonOfficers =>
      liaisonOfficers.liaisonOfficers.nonEmpty && liaisonOfficers.liaisonOfficers.forall(!_.inProgress)
    }

  private def areSignatoriesComplete(journeyData: JourneyData): Boolean =
    journeyData.signatories.exists { signatories =>
      signatories.signatories.nonEmpty && signatories.signatories.forall(!_.inProgress)
    }

  private def areThirdPartyOrganisationsComplete(journeyData: JourneyData): Boolean =
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
