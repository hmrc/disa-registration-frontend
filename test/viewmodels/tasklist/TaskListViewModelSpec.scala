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
import models.YesNoAnswer
import models.journeydata.certificatesofauthority.CertificatesOfAuthority
import models.journeydata.certificatesofauthority.CertificatesOfAuthorityYesNo.Yes as CertificatesYes
import models.journeydata.certificatesofauthority.FcaArticles.Article14
import models.journeydata.isaproducts.IsaProduct.CashIsas
import models.journeydata.isaproducts.IsaProducts
import models.journeydata.liaisonofficers.LiaisonOfficerCommunication.ByEmail
import models.journeydata.liaisonofficers.{LiaisonOfficer, LiaisonOfficers}
import models.journeydata.signatories.{Signatories, Signatory}
import models.journeydata.thirdparty.{ThirdParty, ThirdPartyOrganisations}
import models.journeydata.{JourneyData, OrganisationDetails, OrganisationEmail}
import uk.gov.hmrc.auth.core.{Assistant, User}

class TaskListViewModelSpec extends SpecBase {

  "TaskListViewModel" - {

    "must lock all tasks except organisation information until organisation information is complete" in {
      val viewModel = TaskListViewModel(emptyJourneyDataWithBv, User)

      task(viewModel, "taskList.organisationInformation.add").href.value mustBe
        controllers.orgdetails.routes.RegisteredIsaManagerController.onPageLoad(models.NormalMode).url
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
        "taskList.thirdPartyOrganisations.add",
        "taskList.submit.checkAnswers"
      ).foreach { messageKey =>
        val row = task(viewModel, messageKey)

        row.href mustBe None
        row.status.content mustBe messages("taskList.status.cannotStartYet")
      }
    }

    "must show organisation information in progress while keeping remaining tasks locked" in {
      val journeyData = emptyJourneyDataWithBv.copy(
        organisationDetails = Some(OrganisationDetails(registeredToManageIsa = Some(YesNoAnswer.Yes)))
      )

      val viewModel = TaskListViewModel(journeyData, User)

      task(viewModel, "taskList.organisationInformation.add").status.content mustBe messages(
        "taskList.status.inProgress"
      )
      task(viewModel, "taskList.organisationInformation.add").status.tagClass mustBe Some("govuk-tag--blue")
      task(viewModel, "taskList.organisationEmail.add").href mustBe None
      task(viewModel, "taskList.organisationEmail.add").status.content mustBe messages("taskList.status.cannotStartYet")
    }

    "must unlock remaining tasks once organisation information is complete" in {
      val viewModel =
        TaskListViewModel(emptyJourneyDataWithBv.copy(organisationDetails = Some(completeOrgDetails)), User)

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
        emptyJourneyDataWithBv.copy(
          organisationDetails = Some(completeOrgDetails),
          organisationEmail = Some(OrganisationEmail(Some("test@example.com"), Some(false)))
        ),
        User
      )

      val row = task(viewModel, "taskList.organisationEmail.add")

      row.status.content mustBe messages("taskList.status.notVerified")
      row.status.tagClass mustBe Some("govuk-tag--yellow")
    }

    "must route empty multi-item tasks to the first question" in {
      val viewModel =
        TaskListViewModel(emptyJourneyDataWithBv.copy(organisationDetails = Some(completeOrgDetails)), User)

      task(viewModel, "taskList.liaisonOfficers.add").href.value mustBe
        controllers.liaisonofficers.routes.LiaisonOfficerNameController
          .onPageLoad(id = None, mode = models.NormalMode, returnTo = None)
          .url
      task(viewModel, "taskList.signatories.add").href.value mustBe
        controllers.signatories.routes.SignatoryNameController
          .onPageLoad(id = None, mode = models.NormalMode, returnTo = None)
          .url
      task(viewModel, "taskList.thirdPartyOrganisations.add").href.value mustBe
        controllers.thirdparty.routes.ProductsManagedByThirdPartyController
          .onPageLoad(models.NormalMode)
          .url
    }

    "must route multi-item tasks with existing items to their multiple item CYA pages" in {
      val viewModel = TaskListViewModel(
        emptyJourneyDataWithBv.copy(
          organisationDetails = Some(completeOrgDetails),
          liaisonOfficers = Some(LiaisonOfficers(Seq(LiaisonOfficer("lo-1", fullName = Some("Started"))))),
          signatories = Some(Signatories(Seq(Signatory("sig-1", fullName = Some("Started"))))),
          thirdPartyOrganisations = Some(
            ThirdPartyOrganisations(
              managedByThirdParty = Some(YesNoAnswer.Yes),
              thirdParties = Seq(ThirdParty("third-party-1", thirdPartyName = Some("Started")))
            )
          )
        ),
        User
      )

      task(viewModel, "taskList.liaisonOfficers.change").href.value mustBe
        controllers.liaisonofficers.routes.AddedLiaisonOfficersController.onPageLoad(models.NormalMode).url
      task(viewModel, "taskList.signatories.change").href.value mustBe
        controllers.signatories.routes.AddedSignatoryController.onPageLoad(models.NormalMode).url
      task(viewModel, "taskList.thirdPartyOrganisations.change").href.value mustBe
        controllers.thirdparty.routes.AddedThirdPartiesController.onPageLoad(models.NormalMode).url
    }

    "must route multiple completed third party organisations to the section final CYA" in {
      val viewModel = TaskListViewModel(
        emptyJourneyDataWithBv.copy(
          organisationDetails = Some(completeOrgDetails),
          thirdPartyOrganisations = Some(
            ThirdPartyOrganisations(
              managedByThirdParty = Some(YesNoAnswer.Yes),
              thirdParties = Seq(completeThirdParty("third-party-1"), completeThirdParty("third-party-2"))
            )
          )
        ),
        User
      )

      task(viewModel, "taskList.thirdPartyOrganisations.change").href.value mustBe
        controllers.thirdparty.routes.ThirdPartiesCheckYourAnswersController.onPageLoad().url
    }

    "must show in progress when authorised users or third parties include incomplete records" in {
      val viewModel = TaskListViewModel(
        emptyJourneyDataWithBv.copy(
          organisationDetails = Some(completeOrgDetails),
          liaisonOfficers = Some(
            LiaisonOfficers(
              Seq(
                LiaisonOfficer("lo-1", fullName = Some("Started")),
                completeLiaisonOfficer("lo-2")
              )
            )
          ),
          signatories = Some(
            Signatories(
              Seq(
                Signatory("sig-1", fullName = Some("Started")),
                completeSignatory("sig-2")
              )
            )
          ),
          thirdPartyOrganisations = Some(
            ThirdPartyOrganisations(
              managedByThirdParty = Some(YesNoAnswer.Yes),
              thirdParties = Seq(ThirdParty("third-party-1", thirdPartyName = Some("Started")))
            )
          )
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
        emptyJourneyDataWithBv.copy(
          organisationDetails = Some(completeOrgDetails),
          liaisonOfficers = Some(LiaisonOfficers(Seq(completeLiaisonOfficer("lo-1"), completeLiaisonOfficer("lo-2")))),
          signatories = Some(Signatories(Seq(completeSignatory("sig-1"), completeSignatory("sig-2")))),
          thirdPartyOrganisations = Some(
            ThirdPartyOrganisations(
              managedByThirdParty = Some(YesNoAnswer.Yes),
              thirdParties = Seq(completeThirdParty("third-party-1"), completeThirdParty("third-party-2"))
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
        emptyJourneyDataWithBv.copy(
          organisationDetails = Some(completeOrgDetails),
          thirdPartyOrganisations = Some(ThirdPartyOrganisations(managedByThirdParty = Some(YesNoAnswer.No)))
        ),
        User
      )

      task(viewModel, "taskList.thirdPartyOrganisations.change").status.content mustBe messages(
        "taskList.status.completed"
      )
    }

    "must allow administrators to submit only when all required tasks are complete" in {
      val journeyData = completeJourneyData
      val viewModel   = TaskListViewModel(journeyData, User)

      viewModel.canSubmitAnswers mustBe true
      task(viewModel, "taskList.submit.checkAnswers").href.value mustBe controllers.routes.SubmissionCyaController
        .onPageLoad()
        .url
      task(viewModel, "taskList.submit.checkAnswers").status.content mustBe messages("taskList.status.notYetStarted")
      TaskListViewModel.canSubmitAnswers(journeyData, User) mustBe true
      TaskListViewModel.canSubmitAnswers(journeyData.copy(businessVerification = None), User) mustBe false
    }

    "must prevent assistants from submitting final answers" in {
      val viewModel = TaskListViewModel(completeJourneyData, Assistant)

      viewModel.canSubmitAnswers mustBe false
      val row = task(viewModel, "taskList.submit.assistantCannotSubmit")

      row.href mustBe None
      row.status.toGovuk mustBe uk.gov.hmrc.govukfrontend.views.viewmodels.tasklist.TaskListItemStatus()
      TaskListViewModel.canSubmitAnswers(completeJourneyData, Assistant) mustBe false
    }

    "must require successful business verification before the task list can be accessed" in {
      TaskListViewModel.canAccessTaskList(emptyJourneyData) mustBe false
      TaskListViewModel.canAccessTaskList(emptyJourneyDataWithBv) mustBe true
      TaskListViewModel.canAccessTaskList(
        emptyJourneyDataWithBv.copy(businessVerification = Some(testBV.copy(businessVerificationPassed = Some(false))))
      ) mustBe false
    }
  }

  private def task(viewModel: TaskListViewModel, titleMessageKey: String): TaskListTaskViewModel =
    viewModel.sections.flatMap(_.tasks).find(_.title == messages(titleMessageKey)).value

  private def emptyJourneyDataWithBv: JourneyData =
    emptyJourneyData.copy(businessVerification = Some(testBV))

  private def completeJourneyData: JourneyData =
    emptyJourneyDataWithBv.copy(
      organisationDetails = Some(completeOrgDetails),
      organisationEmail = Some(OrganisationEmail(Some("test@example.com"), Some(true))),
      isaProducts = Some(IsaProducts(isaProducts = Some(Seq(CashIsas)))),
      certificatesOfAuthority = Some(
        CertificatesOfAuthority(certificatesYesNo = Some(CertificatesYes), fcaArticles = Some(Seq(Article14)))
      ),
      liaisonOfficers = Some(LiaisonOfficers(Seq(completeLiaisonOfficer("lo-1")))),
      signatories = Some(Signatories(Seq(completeSignatory("sig-1")))),
      thirdPartyOrganisations = Some(ThirdPartyOrganisations(managedByThirdParty = Some(YesNoAnswer.No)))
    )

  private val completeOrgDetails: OrganisationDetails =
    OrganisationDetails(
      registeredToManageIsa = Some(YesNoAnswer.No),
      tradingUsingDifferentName = Some(YesNoAnswer.No),
      fcaNumber = Some("123456"),
      registeredAddressCorrespondence = Some(YesNoAnswer.Yes),
      orgTelephoneNumber = Some("01234567890")
    )

  private def completeLiaisonOfficer(id: String): LiaisonOfficer =
    LiaisonOfficer(
      id = id,
      fullName = Some("Complete Liaison Officer"),
      phoneNumber = Some("01234567890"),
      communication = Set(ByEmail),
      email = Some("liaison@example.com")
    )

  private def completeSignatory(id: String): Signatory =
    Signatory(
      id = id,
      fullName = Some("Complete Signatory"),
      jobTitle = Some("Director")
    )

  private def completeThirdParty(id: String): ThirdParty =
    ThirdParty(
      id = id,
      thirdPartyName = Some("Complete Third Party"),
      managingIsaReturns = Some(YesNoAnswer.No),
      usingInvestorFunds = Some(YesNoAnswer.No)
    )
}
