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

package controllers

import base.SpecBase
import models.journeydata.JourneyData
import models.journeydata.{OrganisationDetails, OrganisationEmail}
import models.journeydata.certificatesofauthority.CertificatesOfAuthority
import models.journeydata.isaproducts.IsaProducts
import models.journeydata.liaisonofficers.LiaisonOfficers
import models.journeydata.signatories.Signatories
import models.journeydata.thirdparty.ThirdPartyOrganisations
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, reset, verify, when}
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.AuditService
import uk.gov.hmrc.auth.core.{Assistant, CredentialRole, User}
import viewmodels.tasklist.TaskListViewModel
import views.html.TaskListView

import scala.concurrent.Future

class TaskListControllerSpec extends SpecBase {

  private def taskListApplicationBuilder(
    journeyData: Option[JourneyData],
    credentialRole: CredentialRole = User
  ) =
    applicationBuilder(
      journeyData = journeyData,
      credentialRole = credentialRole,
      overrides = Seq(inject.bind[AuditService].toInstance(mockAuditService))
    )

  "TaskListController" - {

    "must return OK and render the view when UPRN enrichment succeeds" in {
      when(mockRegisteredAddressUprnService.enrichUprnIfMissing(any(), any(), any())(any()))
        .thenReturn(Future.successful(()))

      val journeyData = emptyJourneyDataWithBusinessVerification
      val application = taskListApplicationBuilder(
        journeyData = Some(journeyData)
      ).build()

      running(application) {

        val request =
          FakeRequest(GET, routes.TaskListController.onPageLoad().url)
            .withSession("authToken" -> "mock-bearer-token")

        val result = route(application, request).value

        val view = application.injector.instanceOf[TaskListView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual
          view(TaskListViewModel(journeyData, testCredentialRoleUser)(messages(application)))(
            request,
            messages(application)
          ).toString
      }
    }

    "must still return OK even if UPRN enrichment fails (non-blocking)" in {
      when(mockRegisteredAddressUprnService.enrichUprnIfMissing(any(), any(), any())(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val journeyData = emptyJourneyDataWithBusinessVerification
      val application = taskListApplicationBuilder(
        journeyData = Some(journeyData)
      ).build()

      running(application) {

        val request =
          FakeRequest(GET, routes.TaskListController.onPageLoad().url)
            .withSession("authToken" -> "mock-bearer-token")

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Start when no journey data exists" in {
      when(mockRegisteredAddressUprnService.enrichUprnIfMissing(any(), any(), any())(any()))
        .thenReturn(Future.successful(()))

      val application = taskListApplicationBuilder(
        journeyData = None
      ).build()

      running(application) {

        val request =
          FakeRequest(GET, routes.TaskListController.onPageLoad().url)
            .withSession("authToken" -> "mock-bearer-token")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.StartController.onPageLoad().url
      }
    }

    "must redirect to Start when business verification has not succeeded" in {
      when(mockRegisteredAddressUprnService.enrichUprnIfMissing(any(), any(), any())(any()))
        .thenReturn(Future.successful(()))

      val journeyData = emptyJourneyDataWithFailedBusinessVerification
      val application = taskListApplicationBuilder(
        journeyData = Some(journeyData)
      ).build()

      running(application) {

        val request =
          FakeRequest(GET, routes.TaskListController.onPageLoad().url)
            .withSession("authToken" -> "mock-bearer-token")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.StartController.onPageLoad().url
      }
    }

    "must redirect from a valid task-list continuation link and emit the section audit" in {
      val testCases = Seq(
        (
          OrganisationDetails.sectionName,
          emptyJourneyDataWithBusinessVerification,
          OrganisationDetails.sectionName,
          controllers.orgdetails.routes.RegisteredIsaManagerController.onPageLoad(models.NormalMode).url
        ),
        (
          OrganisationDetails.sectionName,
          emptyJourneyDataWithBusinessVerification.copy(organisationDetails =
            Some(completeTaskListOrganisationDetails)
          ),
          OrganisationDetails.sectionName,
          controllers.orgdetails.routes.OrganisationDetailsCheckYourAnswersController.onPageLoad().url
        ),
        (
          OrganisationEmail.sectionName,
          completeTaskListJourneyData.copy(organisationEmail = Some(unverifiedTaskListOrganisationEmail)),
          OrganisationEmail.sectionName,
          controllers.orgemail.routes.EmailVerificationCodeController.onPageLoad(models.NormalMode).url
        ),
        (
          OrganisationEmail.sectionName,
          completeTaskListJourneyData,
          OrganisationEmail.sectionName,
          controllers.orgemail.routes.OrganisationEmailCyaController.onPageLoad().url
        ),
        (
          IsaProducts.sectionName,
          completeTaskListJourneyData,
          IsaProducts.sectionName,
          controllers.isaproducts.routes.IsaProductsCheckYourAnswersController.onPageLoad().url
        ),
        (
          CertificatesOfAuthority.sectionName,
          completeTaskListJourneyData,
          CertificatesOfAuthority.sectionName,
          controllers.certificatesofauthority.routes.CoaCheckYourAnswersController.onPageLoad().url
        ),
        (
          LiaisonOfficers.sectionName,
          completeTaskListJourneyData,
          LiaisonOfficers.sectionName,
          controllers.liaisonofficers.routes.AddedLiaisonOfficersController.onPageLoad(models.NormalMode).url
        ),
        (
          Signatories.sectionName,
          completeTaskListJourneyData,
          Signatories.sectionName,
          controllers.signatories.routes.AddedSignatoryController.onPageLoad(models.NormalMode).url
        ),
        (
          ThirdPartyOrganisations.sectionName,
          emptyJourneyDataWithBusinessVerification.copy(organisationDetails =
            Some(completeTaskListOrganisationDetails)
          ),
          ThirdPartyOrganisations.sectionName,
          controllers.thirdparty.routes.ProductsManagedByThirdPartyController.onPageLoad(models.NormalMode).url
        ),
        (
          ThirdPartyOrganisations.sectionName,
          emptyJourneyDataWithBusinessVerification.copy(
            organisationDetails = Some(completeTaskListOrganisationDetails),
            thirdPartyOrganisations = Some(
              testThirdPartyOrganisations(Seq(completeTaskListThirdParty("third-party-1")))
            )
          ),
          ThirdPartyOrganisations.sectionName,
          controllers.thirdparty.routes.AddedThirdPartiesController.onPageLoad(models.NormalMode).url
        ),
        (
          ThirdPartyOrganisations.sectionName,
          completeTaskListJourneyData,
          ThirdPartyOrganisations.sectionName,
          controllers.thirdparty.routes.ProductsManagedByThirdPartyController.onPageLoad(models.CheckMode).url
        ),
        (
          models.journeydata.DeclareAndSubmit.sectionName,
          completeTaskListJourneyData,
          models.journeydata.DeclareAndSubmit.sectionName,
          routes.SubmissionCyaController.onPageLoad().url
        )
      )

      testCases.foreach { case (taskListSection, journeyData, sectionName, expectedRedirect) =>
        reset(mockAuditService)

        val application =
          taskListApplicationBuilder(journeyData = Some(journeyData)).build()

        running(application) {
          val request = FakeRequest(GET, routes.TaskListController.continueTo(taskListSection).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual expectedRedirect
          verify(mockAuditService).auditContinuation(any(), eqTo(sectionName))
        }
      }
    }

    "must redirect to TaskList and not audit when a continuation link is unavailable" in {
      val application =
        taskListApplicationBuilder(
          journeyData = Some(completeTaskListJourneyData),
          credentialRole = Assistant
        ).build()

      running(application) {
        val request =
          FakeRequest(GET, routes.TaskListController.continueTo(models.journeydata.DeclareAndSubmit.sectionName).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.TaskListController.onPageLoad().url
        verify(mockAuditService, never()).auditContinuation(any(), any())
      }
    }

    "must redirect to TaskList and not audit when the continuation section is unknown" in {
      val application =
        taskListApplicationBuilder(journeyData = Some(completeTaskListJourneyData)).build()

      running(application) {
        val request = FakeRequest(GET, routes.TaskListController.continueTo("unknown-section").url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.TaskListController.onPageLoad().url
        verify(mockAuditService, never()).auditContinuation(any(), any())
      }
    }
  }
}
