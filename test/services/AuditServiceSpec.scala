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
import models.requests.IdentifierRequest
import models.submission.SubmissionResult
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import play.api.inject
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.User
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Disabled, Failure, Success}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.Future

class AuditServiceSpec extends SpecBase {

  private val service = applicationBuilder(
    Some(emptyJourneyData),
    inject.bind[AuditConnector].toInstance(mockAuditConnector),
    inject.bind[FrontendAppConfig].toInstance(mockAppConfig)
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
      (detail \ EventData.internalRegId.toString).as[String] mustEqual jd.enrolmentId
      (detail \ EventData.credentialRole.toString).as[String] mustEqual credentialRole.toString
      (detail \ EventData.groupId.toString).as[String] mustEqual jd.groupId
      (detail \ EventData.submissionStatus.toString).as[String] mustEqual SubmissionResult.Success.toString

      (detail \ EventData.payload.toString).toOption.isDefined mustEqual true

      (detail \ EventData.failureReason.toString).toOption mustEqual None
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
      (detail \ EventData.internalRegId.toString).as[String] mustEqual testEnrolmentId
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
