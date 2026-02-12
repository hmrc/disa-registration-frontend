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

package controllers.actions

import base.SpecBase
import handlers.ErrorHandler
import models.GetOrCreateJourneyData
import models.journeydata.JourneyData
import models.requests.{DataRequest, IdentifierRequest}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import play.api.mvc.Results.InternalServerError
import play.api.mvc.{RequestHeader, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{AuditService, JourneyAnswersService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class GetOrCreateJourneyDataActionSpec extends SpecBase {

  class Harness(
    journeyAnswersService: JourneyAnswersService,
    auditService: AuditService,
    errorHandler: ErrorHandler
  ) extends GetOrCreateJourneyDataActionImpl(journeyAnswersService, auditService, errorHandler) {

    def callRefine[A](request: IdentifierRequest[A]): Future[Either[Result, DataRequest[A]]] =
      refine(request)
  }

  "GetOrCreateJourneyDataAction" - {

    "when a NEW enrolment journey is created" - {

      "must build a DataRequest and fire auditNewEnrolmentStarted" in {
        val jd = JourneyData(groupId = testGroupId, enrolmentId = testEnrolmentId)

        val response = GetOrCreateJourneyData(
          isNewEnrolmentJourney = true,
          journeyData = jd
        )

        when(mockJourneyAnswersService.getOrCreateJourneyData(eqTo(testGroupId))(any[HeaderCarrier]))
          .thenReturn(Future.successful(response))

        when(
          mockAuditService.auditNewEnrolmentStarted(
            any(),
            any(),
            any[String],
            any[String]
          )(any[HeaderCarrier])
        ).thenReturn(Future.successful(()))

        val action = new Harness(mockJourneyAnswersService, mockAuditService, mockErrorHandler)

        val Right(result) = action
          .callRefine(IdentifierRequest(FakeRequest(), testGroupId, testCredentials, testCredentialRoleUser))
          .futureValue: @unchecked

        result.groupId mustBe testGroupId
        result.credentials mustBe testCredentials
        result.credentialRole mustBe testCredentialRoleUser
        result.journeyData mustBe jd

        verify(mockAuditService).auditNewEnrolmentStarted(
          eqTo(testCredentials),
          eqTo(testCredentialRoleUser),
          eqTo(testEnrolmentId),
          eqTo(testGroupId)
        )(any[HeaderCarrier])
      }

      "must still return Right(DataRequest) even if audit fails" in {
        val jd = JourneyData(groupId = testGroupId, enrolmentId = testEnrolmentId)

        val response = GetOrCreateJourneyData(
          isNewEnrolmentJourney = true,
          journeyData = jd
        )

        when(mockJourneyAnswersService.getOrCreateJourneyData(eqTo(testGroupId))(any[HeaderCarrier]))
          .thenReturn(Future.successful(response))

        when(
          mockAuditService.auditNewEnrolmentStarted(any(), any(), any[String], any[String])(any[HeaderCarrier])
        ).thenReturn(Future.failed(new RuntimeException("fubar")))

        val action = new Harness(mockJourneyAnswersService, mockAuditService, mockErrorHandler)

        val Right(result) = action
          .callRefine(IdentifierRequest(FakeRequest(), testGroupId, testCredentials, testCredentialRoleUser))
          .futureValue: @unchecked

        result.journeyData mustBe jd

        verify(mockAuditService).auditNewEnrolmentStarted(
          eqTo(testCredentials),
          eqTo(testCredentialRoleUser),
          eqTo(testEnrolmentId),
          eqTo(testGroupId)
        )(any[HeaderCarrier])
      }
    }

    "when an EXISTING enrolment journey is found" - {

      "must build a DataRequest and NOT fire auditNewEnrolmentStarted" in {
        val jd = JourneyData(groupId = testGroupId, enrolmentId = testEnrolmentId)

        val response = GetOrCreateJourneyData(
          isNewEnrolmentJourney = false,
          journeyData = jd
        )

        when(mockJourneyAnswersService.getOrCreateJourneyData(eqTo(testGroupId))(any[HeaderCarrier]))
          .thenReturn(Future.successful(response))

        val action = new Harness(mockJourneyAnswersService, mockAuditService, mockErrorHandler)

        val Right(result) = action
          .callRefine(IdentifierRequest(FakeRequest(), testGroupId, testCredentials, testCredentialRoleUser))
          .futureValue: @unchecked

        result.journeyData mustBe jd

        verify(mockAuditService, never()).auditNewEnrolmentStarted(any(), any(), any[String], any[String])(
          any[HeaderCarrier]
        )
      }
    }

    "when getOrCreateJourneyData fails" - {

      "must return Left(InternalServerError) and invoke the errorHandler" in {
        val ex = new RuntimeException("mongo down")

        when(mockJourneyAnswersService.getOrCreateJourneyData(eqTo(testGroupId))(any[HeaderCarrier]))
          .thenReturn(Future.failed(ex))

        when(mockErrorHandler.internalServerError(any[RequestHeader]))
          .thenReturn(Future.successful(InternalServerError))

        val action = new Harness(mockJourneyAnswersService, mockAuditService, mockErrorHandler)

        val Left(result) = action
          .callRefine(IdentifierRequest(FakeRequest(), testGroupId, testCredentials, testCredentialRoleUser))
          .futureValue: @unchecked

        status(Future.successful(result)) mustBe INTERNAL_SERVER_ERROR
        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }
  }
}
