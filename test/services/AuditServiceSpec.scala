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

package services

import base.SpecBase
import config.FrontendAppConfig
import models.journeydata.JourneyData
import models.journeydata.isaproducts.IsaProducts
import models.requests.{DataRequest, IdentifierRequest}
import models.submission.SubmissionResult
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import play.api.inject
import play.api.libs.json.JsObject
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.User
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Disabled, Failure, Success}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.Future

class AuditServiceSpec extends SpecBase {

  private val service = applicationBuilder(
    Some(emptyJourneyData),
    overrides = Seq(
      inject.bind[AuditConnector].toInstance(mockAuditConnector),
      inject.bind[FrontendAppConfig].toInstance(mockAppConfig),
      inject.bind[SessionRepository].toInstance(mockSessionRepository)
    )
  )
    .configure(Map("auditing.enabled" -> "true"))
    .build()
    .injector
    .instanceOf[AuditService]

  private val credentials    = Credentials(providerId = testString, providerType = testString)
  private val credentialRole = User
  private val request        = IdentifierRequest(FakeRequest(), testGroupId, testCredentials, testCredentialRoleUser)

  private def stubAuditResult(result: AuditResult): Unit =
    when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any, any))
      .thenReturn(Future.successful(result))

  private def captureEvent(): ExtendedDataEvent = {
    val captor: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
    verify(mockAuditConnector).sendExtendedEvent(captor.capture())(any, any)
    captor.getValue
  }

  private def stubContinuationCanBeAudited(): Unit =
    when(mockSessionRepository.upsertAndMarkAuditEventSent(eqTo(credentials.providerId)))
      .thenReturn(Future.successful(true))

  private def dataRequest(journeyData: JourneyData = testJourneyData): DataRequest[AnyContent] =
    DataRequest(
      request = FakeRequest(),
      groupId = testGroupId,
      credentials = credentials,
      credentialRole = credentialRole,
      journeyData = journeyData
    )

  "AuditService.auditEnrolmentSubmission" - {

    "must send an EnrolmentSubmitted event with the expected base detail fields" in {

      when(mockAppConfig.appName).thenReturn("disa-registration-frontend")
      stubAuditResult(Success)

      val jd = testJourneyData

      service
        .auditEnrolmentSubmission(
          status = SubmissionResult.Success,
          credentials = credentials,
          credentialRole = credentialRole,
          journeyData = jd,
          failureReason = None
        )
        .futureValue mustEqual ()

      val event = captureEvent()

      event.auditSource mustEqual "disa-registration-frontend"
      event.auditType mustEqual AuditTypes.EnrolmentSubmitted.toString

      val detail = event.detail.as[JsObject]

      (detail \ EventData.credId.toString).as[String] mustEqual testString
      (detail \ EventData.providerType.toString).as[String] mustEqual testString
      (detail \ EventData.credentialRole.toString).as[String] mustEqual credentialRole.toString
      (detail \ EventData.submissionStatus.toString).as[String] mustEqual SubmissionResult.Success.toString
      (detail \ EventData.numberLiasonOfficers.toString).as[String] mustEqual "1"
      (detail \ EventData.numberSignatories.toString).as[String] mustEqual "1"
      (detail \ EventData.payload.toString \ EventData.groupName.toString).as[String] mustEqual "unknown"

      (detail \ EventData.payload.toString).toOption.isDefined mustEqual true

      (detail \ EventData.failureReason.toString).toOption mustEqual None
    }

    "must send an EnrolmentStarted event with groupName obtained from GRS" in {
      when(mockAppConfig.appName).thenReturn("disa-registration-frontend")
      stubAuditResult(Success)

      val journeyData =
        testJourneyData.copy(businessVerification = Some(testBV))

      service
        .auditEnrolmentSubmission(
          status = SubmissionResult.Success,
          credentials = credentials,
          credentialRole = credentialRole,
          journeyData = journeyData,
          failureReason = None
        )
        .futureValue mustEqual ()

      val event = captureEvent()

      val detail = event.detail.as[JsObject]

      (detail \ EventData.payload.toString \ EventData.groupName.toString).as[String] mustEqual testString
    }

    "must include subscriptionId in the payload when formBundleId is present" in {
      when(mockAppConfig.appName).thenReturn("disa-registration-frontend")
      stubAuditResult(Success)

      val journeyData =
        testJourneyData.copy(formBundleId = Some(testFormBundleId))

      service
        .auditEnrolmentSubmission(
          status = SubmissionResult.Success,
          credentials = credentials,
          credentialRole = credentialRole,
          journeyData = journeyData,
          failureReason = None
        )
        .futureValue mustEqual ()

      val event  = captureEvent()
      val detail = event.detail.as[JsObject]

      (detail \ EventData.payload.toString \ "subscriptionId").as[String] mustEqual testFormBundleId
    }

    "must include failureReason when status is Failure and a failure reason is provided" in {

      when(mockAppConfig.appName).thenReturn("disa-registration-frontend")
      stubAuditResult(Success)

      val jd     = testJourneyData
      val reason = "ETMP returned 503"

      service
        .auditEnrolmentSubmission(
          status = SubmissionResult.Failure,
          credentials = credentials,
          credentialRole = credentialRole,
          journeyData = jd,
          failureReason = Some(reason)
        )
        .futureValue mustEqual ()

      val event  = captureEvent()
      val detail = event.detail.as[JsObject]

      (detail \ EventData.failureReason.toString).as[String] mustEqual reason
    }

    "must not include failureReason when status is Failure but no reason is provided" in {

      when(mockAppConfig.appName).thenReturn("disa-registration-frontend")
      stubAuditResult(Success)

      val jd = testJourneyData

      service
        .auditEnrolmentSubmission(
          status = SubmissionResult.Failure,
          credentials = credentials,
          credentialRole = credentialRole,
          journeyData = jd,
          failureReason = None
        )
        .futureValue mustEqual (())

      val event  = captureEvent()
      val detail = event.detail.as[JsObject]

      (detail \ EventData.failureReason.toString).toOption mustEqual None
    }

    "must complete successfully even when auditing is Disabled" in {

      when(mockAppConfig.appName).thenReturn("disa-registration-frontend")
      stubAuditResult(Disabled)

      service
        .auditEnrolmentSubmission(
          status = SubmissionResult.Success,
          credentials = credentials,
          credentialRole = credentialRole,
          journeyData = testJourneyData,
          failureReason = None
        )
        .futureValue mustEqual ()
    }

    "must complete successfully even when auditing returns Failure" in {

      when(mockAppConfig.appName).thenReturn("disa-registration-frontend")
      stubAuditResult(Failure("fubar", None))

      service
        .auditEnrolmentSubmission(
          status = SubmissionResult.Success,
          credentials = credentials,
          credentialRole = credentialRole,
          journeyData = testJourneyData,
          failureReason = None
        )
        .futureValue mustEqual ()
    }
  }

  "AuditService.auditContinuation" - {

    "must send an EnrolmentStarted event with journeyType=continueEnrolment and include continuingSection" in {
      when(mockAppConfig.appName).thenReturn("disa-registration-frontend")
      stubContinuationCanBeAudited()
      stubAuditResult(Success)

      val sectionName = IsaProducts.sectionName

      service
        .auditContinuation(
          request = dataRequest(),
          sectionName = sectionName
        )
        .futureValue mustEqual ()

      verify(mockSessionRepository).upsertAndMarkAuditEventSent(eqTo(credentials.providerId))
      val event = captureEvent()

      event.auditSource mustEqual "disa-registration-frontend"
      event.auditType mustEqual AuditTypes.EnrolmentStarted.toString

      val detail = event.detail.as[JsObject]

      (detail \ EventData.credId.toString).as[String] mustEqual credentials.providerId
      (detail \ EventData.providerType.toString).as[String] mustEqual credentials.providerType
      (detail \ EventData.internalRegistrationId.toString).as[String] mustEqual testEnrolmentId
      (detail \ EventData.credentialRole.toString).as[String] mustEqual credentialRole.toString
      (detail \ EventData.groupId.toString).as[String] mustEqual testGroupId
      (detail \ EventData.groupName.toString).as[String] mustEqual "unknown"

      (detail \ EventData.journeyType.toString).as[String] mustEqual EventData.continueEnrolment.toString
      (detail \ EventData.continuingSection.toString).as[String] mustEqual sectionName
    }

    "must send an EnrolmentStarted event with groupName obtained from GRS" in {
      when(mockAppConfig.appName).thenReturn("disa-registration-frontend")
      stubContinuationCanBeAudited()
      stubAuditResult(Success)

      val sectionName = IsaProducts.sectionName

      val journeyData =
        testJourneyData.copy(businessVerification = Some(testBV))

      service
        .auditContinuation(
          request = dataRequest(journeyData),
          sectionName = sectionName
        )
        .futureValue mustEqual ()

      verify(mockSessionRepository).upsertAndMarkAuditEventSent(eqTo(credentials.providerId))
      val event  = captureEvent()
      val detail = event.detail.as[JsObject]

      (detail \ EventData.groupName.toString).as[String] mustEqual testString
    }

    "must complete successfully even when auditing is Disabled" in {
      when(mockAppConfig.appName).thenReturn("disa-registration-frontend")
      stubContinuationCanBeAudited()
      stubAuditResult(Disabled)

      val sectionName = IsaProducts.sectionName

      service
        .auditContinuation(
          request = dataRequest(),
          sectionName = sectionName
        )
        .futureValue mustEqual ()

      verify(mockSessionRepository).upsertAndMarkAuditEventSent(eqTo(credentials.providerId))
      verify(mockAuditConnector).sendExtendedEvent(any[ExtendedDataEvent])(any, any)
    }

    "must complete successfully even when auditing returns Failure" in {
      when(mockAppConfig.appName).thenReturn("disa-registration-frontend")
      stubContinuationCanBeAudited()
      stubAuditResult(Failure("fubar", None))

      val sectionName = IsaProducts.sectionName

      service
        .auditContinuation(
          request = dataRequest(),
          sectionName = sectionName
        )
        .futureValue mustEqual ()

      verify(mockSessionRepository).upsertAndMarkAuditEventSent(eqTo(credentials.providerId))
      verify(mockAuditConnector).sendExtendedEvent(any[ExtendedDataEvent])(any, any)
    }

    "must not send an event when the continuation audit has already been sent" in {
      when(mockSessionRepository.upsertAndMarkAuditEventSent(eqTo(credentials.providerId)))
        .thenReturn(Future.successful(false))

      service
        .auditContinuation(
          request = dataRequest(),
          sectionName = IsaProducts.sectionName
        )
        .futureValue mustEqual ()

      verify(mockSessionRepository).upsertAndMarkAuditEventSent(eqTo(credentials.providerId))
      verify(mockAuditConnector, never()).sendExtendedEvent(any[ExtendedDataEvent])(any, any)
    }

    "must recover when the session audit marker fails" in {
      when(mockSessionRepository.upsertAndMarkAuditEventSent(eqTo(credentials.providerId)))
        .thenReturn(Future.failed(new RuntimeException("fubar")))

      service
        .auditContinuation(
          request = dataRequest(),
          sectionName = IsaProducts.sectionName
        )
        .futureValue mustEqual ()

      verify(mockSessionRepository).upsertAndMarkAuditEventSent(eqTo(credentials.providerId))
      verify(mockAuditConnector, never()).sendExtendedEvent(any[ExtendedDataEvent])(any, any)
    }

    "must recover when sending the audit event fails" in {
      when(mockAppConfig.appName).thenReturn("disa-registration-frontend")
      stubContinuationCanBeAudited()
      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any, any))
        .thenReturn(Future.failed(new RuntimeException("fubar")))

      service
        .auditContinuation(
          request = dataRequest(),
          sectionName = IsaProducts.sectionName
        )
        .futureValue mustEqual ()

      verify(mockSessionRepository).upsertAndMarkAuditEventSent(eqTo(credentials.providerId))
      verify(mockAuditConnector).sendExtendedEvent(any[ExtendedDataEvent])(any, any)
    }
  }

  "AuditService.auditNewEnrolmentStarted" - {

    "must send an EnrolmentStarted event with the expected base detail fields" in {
      when(mockAppConfig.appName).thenReturn("disa-registration-frontend")
      stubAuditResult(Success)

      service
        .auditNewEnrolmentStarted(
          request = request,
          journeyData = testJourneyData
        )
        .futureValue mustEqual ()

      val event = captureEvent()

      event.auditSource mustEqual "disa-registration-frontend"
      event.auditType mustEqual AuditTypes.EnrolmentStarted.toString

      val detail = event.detail.as[JsObject]

      (detail \ EventData.credId.toString).as[String] mustEqual credentials.providerId
      (detail \ EventData.providerType.toString).as[String] mustEqual credentials.providerType
      (detail \ EventData.internalRegistrationId.toString).as[String] mustEqual testEnrolmentId
      (detail \ EventData.credentialRole.toString).as[String] mustEqual credentialRole.toString
      (detail \ EventData.groupId.toString).as[String] mustEqual testGroupId
      (detail \ EventData.journeyType.toString).as[String] mustEqual EventData.startEnrolment.toString
    }

    "must complete successfully even when auditing is Disabled" in {
      when(mockAppConfig.appName).thenReturn("disa-registration-frontend")
      stubAuditResult(Disabled)

      service
        .auditNewEnrolmentStarted(
          request = request,
          journeyData = testJourneyData
        )
        .futureValue mustEqual ()
    }

    "must complete successfully even when auditing returns Failure" in {
      when(mockAppConfig.appName).thenReturn("disa-registration-frontend")
      stubAuditResult(Failure("fubar", None))

      service
        .auditNewEnrolmentStarted(
          request = request,
          journeyData = testJourneyData
        )
        .futureValue mustEqual ()
    }
  }
}
