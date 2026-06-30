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

import base.SpecBase
import models.journeydata.{DeclareAndSubmit, OrganisationDetails, TaskListProgress}
import models.journeydata.liaisonofficers.LiaisonOfficers
import models.journeydata.signatories.Signatories
import models.journeydata.thirdparty.ThirdPartyOrganisations
import uk.gov.hmrc.auth.core.{Assistant, User}

class TaskListViewModelSpec extends SpecBase {

  "TaskListViewModel" - {

    "must unlock all task sections except submit after successful business verification" in {
      val viewModel = TaskListViewModel(emptyJourneyDataWithBusinessVerification, User)

      task(viewModel, "taskList.organisationInformation.add").href.value mustBe
        continueUrl(OrganisationDetails.sectionName)
      task(viewModel, "taskList.organisationInformation.add").status.content mustBe messages(
        "taskList.status.notYetStarted"
      )
      task(viewModel, "taskList.organisationInformation.add").status.tagClass mustBe Some("govuk-tag--blue")

      Seq(
        "taskList.organisationEmail.add",
        "taskList.isaProducts.add",
        "taskList.certificatesOfAuthority.add",
        "taskList.liaisonOfficers.add",
        "taskList.signatories.add",
        "taskList.thirdPartyOrganisations.add"
      ).foreach { messageKey =>
        val row = task(viewModel, messageKey)

        row.href must not be empty
        row.status.content mustBe messages("taskList.status.notYetStarted")
        row.status.tagClass mustBe Some("govuk-tag--blue")
      }

      val submitRow = task(viewModel, "taskList.submit.checkAnswers")
      submitRow.href mustBe None
      submitRow.status.content mustBe messages("taskList.status.cannotStartYet")
    }

    "must show organisation information in progress while keeping remaining task sections accessible" in {
      val journeyData = emptyJourneyDataWithBusinessVerification.copy(
        organisationDetails = Some(inProgressTaskListOrganisationDetails)
      )

      val viewModel = TaskListViewModel(journeyData, User)

      task(viewModel, "taskList.organisationInformation.add").status.content mustBe messages(
        "taskList.status.inProgress"
      )
      task(viewModel, "taskList.organisationInformation.add").status.tagClass mustBe Some("govuk-tag--blue")
      task(viewModel, "taskList.organisationEmail.add").href must not be empty
      task(viewModel, "taskList.organisationEmail.add").status.content mustBe messages("taskList.status.notYetStarted")
    }

    "must mark organisation information complete while keeping other incomplete sections accessible" in {
      val viewModel =
        TaskListViewModel(
          emptyJourneyDataWithBusinessVerification.copy(organisationDetails =
            Some(completeTaskListOrganisationDetails)
          ),
          User
        )

      task(viewModel, "taskList.organisationInformation.change").status.content mustBe messages(
        "taskList.status.completed"
      )

      Seq(
        "taskList.organisationEmail.add",
        "taskList.isaProducts.add",
        "taskList.certificatesOfAuthority.add",
        "taskList.liaisonOfficers.add",
        "taskList.signatories.add",
        "taskList.thirdPartyOrganisations.add"
      ).foreach { messageKey =>
        val row = task(viewModel, messageKey)

        row.href must not be empty
        row.status.content mustBe messages("taskList.status.notYetStarted")
        row.status.tagClass mustBe Some("govuk-tag--blue")
      }
    }

    "must show not verified for an unverified organisation email" in {
      val viewModel = TaskListViewModel(
        emptyJourneyDataWithBusinessVerification.copy(
          organisationDetails = Some(completeTaskListOrganisationDetails),
          organisationEmail = Some(unverifiedTaskListOrganisationEmail)
        ),
        User
      )

      val row = task(viewModel, "taskList.organisationEmail.add")

      row.status.content mustBe messages("taskList.status.notVerified")
      row.status.tagClass mustBe Some("govuk-tag--yellow")
    }

    "must link empty multi-item tasks through task-list continuation routes" in {
      val viewModel =
        TaskListViewModel(
          emptyJourneyDataWithBusinessVerification.copy(organisationDetails =
            Some(completeTaskListOrganisationDetails)
          ),
          User
        )

      task(viewModel, "taskList.liaisonOfficers.add").href.value mustBe
        continueUrl(LiaisonOfficers.sectionName)
      task(viewModel, "taskList.signatories.add").href.value mustBe
        continueUrl(Signatories.sectionName)
      task(viewModel, "taskList.thirdPartyOrganisations.add").href.value mustBe
        continueUrl(ThirdPartyOrganisations.sectionName)
    }

    "must link multi-item tasks with existing items through task-list continuation routes" in {
      val viewModel = TaskListViewModel(
        emptyJourneyDataWithBusinessVerification.copy(
          organisationDetails = Some(completeTaskListOrganisationDetails),
          liaisonOfficers = Some(liaisonOfficersWith(inProgressTaskListLiaisonOfficer("lo-1"))),
          signatories = Some(signatoriesWith(inProgressTaskListSignatory("sig-1"))),
          thirdPartyOrganisations =
            Some(testThirdPartyOrganisations(Seq(inProgressTaskListThirdParty("third-party-1"))))
        ),
        User
      )

      task(viewModel, "taskList.liaisonOfficers.change").href.value mustBe
        continueUrl(LiaisonOfficers.sectionName)
      task(viewModel, "taskList.signatories.change").href.value mustBe
        continueUrl(Signatories.sectionName)
      task(viewModel, "taskList.thirdPartyOrganisations.change").href.value mustBe
        continueUrl(ThirdPartyOrganisations.sectionName)
    }

    "must link multiple completed third party organisations through the task-list continuation route" in {
      val viewModel = TaskListViewModel(
        emptyJourneyDataWithBusinessVerification.copy(
          organisationDetails = Some(completeTaskListOrganisationDetails),
          thirdPartyOrganisations = Some(
            testThirdPartyOrganisations(
              Seq(completeTaskListThirdParty("third-party-1"), completeTaskListThirdParty("third-party-2"))
            )
          )
        ),
        User
      )

      task(viewModel, "taskList.thirdPartyOrganisations.change").href.value mustBe
        continueUrl(ThirdPartyOrganisations.sectionName)
    }

    "must show in progress when authorised users or third parties include incomplete records" in {
      val viewModel = TaskListViewModel(
        emptyJourneyDataWithBusinessVerification.copy(
          organisationDetails = Some(completeTaskListOrganisationDetails),
          liaisonOfficers = Some(
            liaisonOfficersWith(
              inProgressTaskListLiaisonOfficer("lo-1"),
              completeTaskListLiaisonOfficer("lo-2")
            )
          ),
          signatories = Some(
            signatoriesWith(
              inProgressTaskListSignatory("sig-1"),
              completeTaskListSignatory("sig-2")
            )
          ),
          thirdPartyOrganisations =
            Some(testThirdPartyOrganisations(Seq(inProgressTaskListThirdParty("third-party-1"))))
        ),
        User
      )

      Seq(
        "taskList.liaisonOfficers.change",
        "taskList.signatories.change",
        "taskList.thirdPartyOrganisations.change"
      ).foreach { messageKey =>
        val row = task(viewModel, messageKey)

        row.status.content mustBe messages("taskList.status.inProgress")
        row.status.tagClass mustBe Some("govuk-tag--blue")
      }
    }

    "must count authorised users and third parties when all records are complete" in {
      val viewModel = TaskListViewModel(
        emptyJourneyDataWithBusinessVerification.copy(
          organisationDetails = Some(completeTaskListOrganisationDetails),
          liaisonOfficers = Some(
            liaisonOfficersWith(completeTaskListLiaisonOfficer("lo-1"), completeTaskListLiaisonOfficer("lo-2"))
          ),
          signatories = Some(signatoriesWith(completeTaskListSignatory("sig-1"), completeTaskListSignatory("sig-2"))),
          thirdPartyOrganisations = Some(
            testThirdPartyOrganisations(
              Seq(completeTaskListThirdParty("third-party-1"), completeTaskListThirdParty("third-party-2"))
            )
          )
        ),
        User
      )

      task(viewModel, "taskList.liaisonOfficers.change").status.content mustBe
        messages(app)("taskList.count.liaisonOfficer.other", 2)
      task(viewModel, "taskList.signatories.change").status.content mustBe
        messages(app)("taskList.count.signatory.other", 2)
      task(viewModel, "taskList.thirdPartyOrganisations.change").status.content mustBe
        messages(app)("taskList.count.thirdParty.other", 2)
    }

    "must mark third party organisations complete without a count when the answer is no" in {
      val viewModel = TaskListViewModel(
        emptyJourneyDataWithBusinessVerification.copy(
          organisationDetails = Some(completeTaskListOrganisationDetails),
          thirdPartyOrganisations = Some(thirdPartyOrganisationsNotUsed)
        ),
        User
      )

      task(viewModel, "taskList.thirdPartyOrganisations.change").status.content mustBe messages(
        "taskList.status.completed"
      )
    }

    "must allow administrators to submit only when all required tasks are complete" in {
      val journeyData = completeTaskListJourneyData
      val viewModel   = TaskListViewModel(journeyData, User)

      viewModel.canSubmitAnswers mustBe true
      task(viewModel, "taskList.submit.checkAnswers").href.value mustBe continueUrl(DeclareAndSubmit.sectionName)
      task(viewModel, "taskList.submit.checkAnswers").status.content mustBe messages("taskList.status.notYetStarted")
      TaskListProgress.canSubmitAnswers(journeyData, User) mustBe true
      TaskListProgress.canSubmitAnswers(
        journeyData.copy(businessVerification = None),
        User
      ) mustBe false
    }

    "must prevent assistants from submitting final answers" in {
      val viewModel = TaskListViewModel(completeTaskListJourneyData, Assistant)

      viewModel.canSubmitAnswers mustBe false
      val row = task(viewModel, "taskList.submit.assistantCannotSubmit")

      row.href mustBe None
      row.status.toGovuk mustBe uk.gov.hmrc.govukfrontend.views.viewmodels.tasklist.TaskListItemStatus()
      TaskListProgress.canSubmitAnswers(completeTaskListJourneyData, Assistant) mustBe false
    }

    "must require successful business verification before the task list can be accessed" in {
      TaskListProgress.canAccessTaskList(emptyJourneyData) mustBe false
      TaskListProgress.canAccessTaskList(emptyJourneyDataWithBusinessVerification) mustBe true
      TaskListProgress.canAccessTaskList(emptyJourneyDataWithFailedBusinessVerification) mustBe false
    }
  }

  private def task(viewModel: TaskListViewModel, titleMessageKey: String): TaskListTaskViewModel =
    viewModel.sections.flatMap(_.tasks).find(_.title == messages(titleMessageKey)).value

  private def continueUrl(sectionName: String): String =
    controllers.routes.TaskListController.continueTo(sectionName).url
}
