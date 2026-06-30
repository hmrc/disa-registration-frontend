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

import controllers.routes.TaskListController
import models.journeydata.{JourneyData, TaskListProgress}
import models.journeydata.certificatesofauthority.CertificatesOfAuthority
import models.journeydata.isaproducts.IsaProducts
import models.journeydata.liaisonofficers.LiaisonOfficers
import models.journeydata.signatories.Signatories
import models.journeydata.thirdparty.ThirdPartyOrganisations
import models.journeydata.{DeclareAndSubmit, OrganisationDetails, OrganisationEmail}
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
    val remainingTasksUnlocked = TaskListProgress.canAccessTaskList(journeyData)
    val submitAnswersAvailable = TaskListProgress.canSubmitAnswers(journeyData, credentialRole)

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

  private def organisationInformationTask(journeyData: JourneyData)(implicit
    messages: Messages
  ): TaskListTaskViewModel = {
    val completed = TaskListProgress.isOrganisationInformationComplete(journeyData)
    val started   = organisationInformationStarted(journeyData)

    TaskListTaskViewModel(
      title =
        if (completed) messages("taskList.organisationInformation.change")
        else messages("taskList.organisationInformation.add"),
      href = Some(
        TaskListController.continueTo(OrganisationDetails.sectionName).url
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
    val verified = TaskListProgress.isOrganisationEmailVerified(journeyData)

    TaskListTaskViewModel(
      title =
        if (verified) messages("taskList.organisationEmail.change")
        else messages("taskList.organisationEmail.add"),
      href = Option.when(unlocked)(TaskListController.continueTo(OrganisationEmail.sectionName).url),
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
      complete = TaskListProgress.isIsaProductsComplete(journeyData),
      started = isaProductsStarted(journeyData),
      addMessage = "taskList.isaProducts.add",
      changeMessage = "taskList.isaProducts.change",
      href = TaskListController.continueTo(IsaProducts.sectionName).url
    )

  private def certificatesOfAuthorityTask(journeyData: JourneyData, unlocked: Boolean)(implicit
    messages: Messages
  ): TaskListTaskViewModel =
    standardTask(
      unlocked = unlocked,
      complete = TaskListProgress.isCertificatesOfAuthorityComplete(journeyData),
      started = certificatesOfAuthorityStarted(journeyData),
      addMessage = "taskList.certificatesOfAuthority.add",
      changeMessage = "taskList.certificatesOfAuthority.change",
      href = TaskListController.continueTo(CertificatesOfAuthority.sectionName).url
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
      href = TaskListController.continueTo(LiaisonOfficers.sectionName).url,
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
      href = TaskListController.continueTo(Signatories.sectionName).url,
      singularMessage = "taskList.count.signatory.one",
      pluralMessage = "taskList.count.signatory.other"
    )
  }

  private def thirdPartyOrganisationsTask(journeyData: JourneyData, unlocked: Boolean)(implicit
    messages: Messages
  ): TaskListTaskViewModel = {
    val section            = journeyData.thirdPartyOrganisations
    val count              = section.fold(0)(_.thirdParties.size)
    val complete           = TaskListProgress.areThirdPartyOrganisationsComplete(journeyData)
    val started            = section.flatMap(_.managedByThirdParty).isDefined
    val hasIncompleteItems =
      section.exists(_.thirdParties.exists(_.inProgress))

    TaskListTaskViewModel(
      title =
        if (complete || count > 0) messages("taskList.thirdPartyOrganisations.change")
        else messages("taskList.thirdPartyOrganisations.add"),
      href = Option.when(unlocked)(TaskListController.continueTo(ThirdPartyOrganisations.sectionName).url),
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
      href = Option.when(canSubmit)(TaskListController.continueTo(DeclareAndSubmit.sectionName).url),
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
    href: String
  )(implicit messages: Messages): TaskListTaskViewModel =
    TaskListTaskViewModel(
      title = if (complete) messages(changeMessage) else messages(addMessage),
      href = Option.when(unlocked)(href),
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
    href: String,
    singularMessage: String,
    pluralMessage: String
  )(implicit messages: Messages): TaskListTaskViewModel =
    TaskListTaskViewModel(
      title = if (count > 0) messages(changeMessage) else messages(addMessage),
      href = Option.when(unlocked)(href),
      status =
        if (!unlocked) plain("taskList.status.cannotStartYet")
        else if (hasIncompleteItems) blueTag("taskList.status.inProgress")
        else if (count > 0) countStatus(count, singularMessage, pluralMessage)
        else blueTag("taskList.status.notYetStarted")
    )

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

  private def isaProductsStarted(journeyData: JourneyData): Boolean =
    journeyData.isaProducts.exists { isaProducts =>
      Seq(
        isaProducts.isaProducts,
        isaProducts.innovativeFinancialProducts,
        isaProducts.p2pPlatform,
        isaProducts.p2pPlatformNumber
      ).exists(_.isDefined)
    }

  private def certificatesOfAuthorityStarted(journeyData: JourneyData): Boolean =
    journeyData.certificatesOfAuthority.exists { certificatesOfAuthority =>
      Seq(
        certificatesOfAuthority.certificatesYesNo,
        certificatesOfAuthority.fcaArticles,
        certificatesOfAuthority.financialOrganisation
      ).exists(_.isDefined)
    }
}
