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
import models.journeydata.isaproducts.IsaProducts
import models.requests.DataRequest
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContent, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class AuditContinuationActionSpec extends SpecBase {

  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  private val sectionName = IsaProducts.sectionName

  private def dataRequest: DataRequest[AnyContent] = {
    DataRequest(
      request = FakeRequest(),
      groupId = testGroupId,
      credentials = testCredentials,
      credentialRole = testCredentialRoleUser,
      journeyData = testJourneyData
    )
  }

  private def runThroughTransformer(
                                     transformer: play.api.mvc.ActionTransformer[DataRequest, DataRequest],
                                     req: DataRequest[AnyContent]
                                   ): Future[(Result, DataRequest[AnyContent])] = {
    var seen: Option[DataRequest[AnyContent]] = None

    val resultF = transformer.invokeBlock(req, { (r: DataRequest[AnyContent]) =>
      seen = Some(r)
      Future.successful(Ok)
    })

    resultF.map(r => (r, seen.getOrElse(fail("Block was not invoked"))))
  }

  "AuditContinuationAction" - {

    "when markAuditEventSent returns true" - {

      "must call auditContinuation and pass request" in {
        when(mockSessionRepository.markAuditEventSent(eqTo(testGroupId)))
          .thenReturn(Future.successful(true))

        when(mockAuditService.auditContinuation(any(), eqTo(sectionName))(any[HeaderCarrier]))
          .thenReturn(Future.successful(()))

        val action      = new AuditContinuationActionImpl(mockSessionRepository, mockAuditService)
        val transformer = action(sectionName)

        val (result, seenReq) =
          runThroughTransformer(transformer, dataRequest).futureValue

        status(Future.successful(result)) mustBe OK
        seenReq.groupId mustBe testGroupId

        verify(mockSessionRepository).markAuditEventSent(eqTo(testGroupId))
        verify(mockAuditService).auditContinuation(any(), eqTo(sectionName))(any[HeaderCarrier])
      }

      "must pass request through even if auditContinuation fails" in {
        when(mockSessionRepository.markAuditEventSent(eqTo(testGroupId)))
          .thenReturn(Future.successful(true))

        when(mockAuditService.auditContinuation(any(), eqTo(sectionName))(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("fubar")))

        val action      = new AuditContinuationActionImpl(mockSessionRepository, mockAuditService)
        val transformer = action(sectionName)

        val (result, seenReq) =
          runThroughTransformer(transformer, dataRequest).futureValue

        status(Future.successful(result)) mustBe OK
        seenReq.groupId mustBe testGroupId

        verify(mockSessionRepository).markAuditEventSent(eqTo(testGroupId))
        verify(mockAuditService).auditContinuation(any(), eqTo(sectionName))(any[HeaderCarrier])
      }
    }

    "when markAuditEventSent returns false" - {

      "must NOT call auditContinuation and pass request unchanged" in {
        when(mockSessionRepository.markAuditEventSent(eqTo(testGroupId)))
          .thenReturn(Future.successful(false))

        val action      = new AuditContinuationActionImpl(mockSessionRepository, mockAuditService)
        val transformer = action(sectionName)

        val (result, seenReq) =
          runThroughTransformer(transformer, dataRequest).futureValue

        status(Future.successful(result)) mustBe OK
        seenReq.groupId mustBe testGroupId

        verify(mockSessionRepository).markAuditEventSent(eqTo(testGroupId))
        verify(mockAuditService, never()).auditContinuation(any(), any())(any[HeaderCarrier])
      }
    }

    "when markAuditEventSent fails" - {

      "must recover, NOT call auditContinuation, and pass request unchanged" in {
        when(mockSessionRepository.markAuditEventSent(eqTo(testGroupId)))
          .thenReturn(Future.failed(new RuntimeException("mongo down")))

        val action      = new AuditContinuationActionImpl(mockSessionRepository, mockAuditService)
        val transformer = action(sectionName)

        val (result, seenReq) =
          runThroughTransformer(transformer, dataRequest).futureValue

        status(Future.successful(result)) mustBe OK
        seenReq.groupId mustBe testGroupId

        verify(mockSessionRepository).markAuditEventSent(eqTo(testGroupId))
        verify(mockAuditService, never()).auditContinuation(any(), any())(any[HeaderCarrier])
      }
    }
  }
}