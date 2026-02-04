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
import connectors.DisaRegistrationConnector
import models.submission.EnrolmentSubmissionResponse
import models.submission.SubmissionResult.{Failure, Success}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import play.api.inject
import uk.gov.hmrc.auth.core.User
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class SubmissionServiceSpec extends SpecBase {

  private val service = applicationBuilder(
    Some(emptyJourneyData),
    inject.bind[DisaRegistrationConnector].toInstance(mockDisaRegistrationConnector),
    inject.bind[AuditService].toInstance(mockAuditService)
  ).build().injector.instanceOf[SubmissionService]

  private val credentials    = Credentials(testString, testString)
  private val credentialRole = User

  "SubmissionService.declareAndSubmit" - {

    "must return receiptId when connector call succeeds and audit success" in {

      val jd        = testJourneyData
      val receiptId = testString

      when(mockDisaRegistrationConnector.declareAndSubmit(eqTo(jd.groupId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(EnrolmentSubmissionResponse(receiptId)))

      when(
        mockAuditService.auditEnrolmentSubmission(
          eqTo(Success),
          eqTo(credentials),
          eqTo(credentialRole),
          eqTo(jd),
          eqTo(None)
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(()))

      val result = service.declareAndSubmit(credentials, credentialRole, jd)(ec, hc).futureValue

      result mustEqual receiptId

      verify(mockDisaRegistrationConnector).declareAndSubmit(eqTo(jd.groupId))(any[HeaderCarrier])
      verify(mockAuditService)
        .auditEnrolmentSubmission(eqTo(Success), eqTo(credentials), eqTo(credentialRole), eqTo(jd), eqTo(None))(
          any[HeaderCarrier]
        )
    }

    "must fail when connector call fails and audit failure with the exception message" in {

      val jd = testJourneyData
      val ex = new RuntimeException("fubar")

      when(mockDisaRegistrationConnector.declareAndSubmit(eqTo(jd.groupId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(ex))

      when(
        mockAuditService.auditEnrolmentSubmission(
          eqTo(Failure),
          eqTo(credentials),
          eqTo(credentialRole),
          eqTo(jd),
          eqTo(Some("fubar"))
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(()))

      val thrown = service.declareAndSubmit(credentials, credentialRole, jd)(ec, hc).failed.futureValue

      thrown mustBe ex

      verify(mockDisaRegistrationConnector).declareAndSubmit(eqTo(jd.groupId))(any[HeaderCarrier])
      verify(mockAuditService).auditEnrolmentSubmission(
        eqTo(Failure),
        eqTo(credentials),
        eqTo(credentialRole),
        eqTo(jd),
        eqTo(Some("fubar"))
      )(any[HeaderCarrier])
    }
  }
}
