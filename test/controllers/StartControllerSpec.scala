/*
 * Copyright 2025 HM Revenue & Customs
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
import models.GetOrCreateEnrolmentResponse
import models.journeydata.{BusinessVerification, JourneyData}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.inject
import services.AuditService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class StartControllerSpec extends SpecBase {

  private def fakeRequest =
    FakeRequest(GET, controllers.routes.StartController.onPageLoad().url)

  "StartController" - {

    "onPageLoad" - {

      "must redirect to TaskList when business verification has passed" in {
        val journeyData = JourneyData(
          groupId = "groupId",
          enrolmentId = "enrolmentId",
          businessVerification = Some(
            BusinessVerification(
              businessRegistrationPassed = Some(true),
              businessVerificationPassed = Some(true),
              ctUtr = Some("1234567890")
            )
          )
        )

        when(mockJourneyAnswersService.getOrCreateEnrolment(any)(any))
          .thenReturn(Future.successful(GetOrCreateEnrolmentResponse(false, journeyData)))

        val application =
          applicationBuilder(journeyData = Some(journeyData)).build()

        running(application) {
          val result = route(application, fakeRequest).value

          status(result)                 shouldBe SEE_OTHER
          redirectLocation(result).value shouldBe
            controllers.routes.TaskListController.onPageLoad().url
        }
      }

      "must redirect to Business Verification lockout when verification has failed" in {
        val journeyData = JourneyData(
          groupId = "groupId",
          enrolmentId = "enrolmentId",
          businessVerification = Some(
            BusinessVerification(
              businessRegistrationPassed = Some(true),
              businessVerificationPassed = Some(false),
              ctUtr = Some("1234567890")
            )
          )
        )

        when(mockJourneyAnswersService.getOrCreateEnrolment(any)(any))
          .thenReturn(Future.successful(GetOrCreateEnrolmentResponse(false, journeyData)))

        val application =
          applicationBuilder(journeyData = Some(journeyData)).build()

        running(application) {
          val result = route(application, fakeRequest).value

          status(result)                 shouldBe SEE_OTHER
          redirectLocation(result).value shouldBe
            controllers.routes.BusinessVerificationController.lockout().url
        }
      }

      "must redirect to GRS journey start when no business verification data exists" in {
        val application =
          applicationBuilder(journeyData = Some(emptyJourneyData)).build()

        when(mockGrsService.getGRSJourneyStartUrl(any[HeaderCarrier], any[RequestHeader]))
          .thenReturn(Future.successful("http://grs-start-url"))

        when(mockJourneyAnswersService.getOrCreateEnrolment(any)(any))
          .thenReturn(Future.successful(GetOrCreateEnrolmentResponse(false, emptyJourneyData)))

        running(application) {
          val result = route(application, fakeRequest).value

          status(result)                 shouldBe SEE_OTHER
          redirectLocation(result).value shouldBe "http://grs-start-url"
        }
      }

      "must redirect to GRS journey start when business verification exists but is incomplete" in {
        val journeyData = JourneyData(
          groupId = "groupId",
          enrolmentId = "enrolmentId",
          businessVerification = Some(
            BusinessVerification(
              businessRegistrationPassed = None,
              businessVerificationPassed = None,
              ctUtr = None
            )
          )
        )

        val application =
          applicationBuilder(journeyData = Some(journeyData)).build()

        when(mockJourneyAnswersService.getOrCreateEnrolment(any)(any))
          .thenReturn(Future.successful(GetOrCreateEnrolmentResponse(false, journeyData)))

        when(mockGrsService.getGRSJourneyStartUrl(any[HeaderCarrier], any[RequestHeader]))
          .thenReturn(Future.successful("http://grs-start-url"))

        running(application) {
          val result = route(application, fakeRequest).value

          status(result)                 shouldBe SEE_OTHER
          redirectLocation(result).value shouldBe "http://grs-start-url"
        }
      }

      "must redirect to Internal Server Error page when GRS service fails" in {
        val application =
          applicationBuilder(journeyData = Some(emptyJourneyData)).build()

        when(mockJourneyAnswersService.getOrCreateEnrolment(any)(any))
          .thenReturn(Future.successful(GetOrCreateEnrolmentResponse(false, emptyJourneyData)))

        when(mockGrsService.getGRSJourneyStartUrl(any[HeaderCarrier], any[RequestHeader]))
          .thenReturn(Future.failed(new Exception("GRS down")))

        running(application) {
          val result = route(application, fakeRequest).value

          status(result)                 shouldBe SEE_OTHER
          redirectLocation(result).value shouldBe
            controllers.routes.InternalServerErrorController.onPageLoad().url
        }
      }

      "must redirect to Internal Server Error page when journey service fails" in {
        val application =
          applicationBuilder(journeyData = Some(emptyJourneyData)).build()

        when(mockJourneyAnswersService.getOrCreateEnrolment(any)(any))
          .thenReturn(Future.failed(new Exception("GRS down")))

        running(application) {
          val result = route(application, fakeRequest).value

          status(result)                 shouldBe SEE_OTHER
          redirectLocation(result).value shouldBe
            controllers.routes.InternalServerErrorController.onPageLoad().url
        }
      }

      "must audit EnrolmentStarted(startEnrolment) when user is new and then redirect appropriately" in {
        val journeyData = JourneyData(
          groupId = testGroupId,
          enrolmentId = testEnrolmentId
        )

        when(mockJourneyAnswersService.getOrCreateEnrolment(any)(any))
          .thenReturn(Future.successful(GetOrCreateEnrolmentResponse(true, journeyData)))

        when(mockAuditService.auditNewEnrolmentStarted(any, any, any, any)(any))
          .thenReturn(Future.unit)

        when(mockGrsService.getGRSJourneyStartUrl(any[HeaderCarrier], any[RequestHeader]))
          .thenReturn(Future.successful("http://grs-start-url"))

        val application =
          applicationBuilder(journeyData = Some(journeyData), inject.bind[AuditService].toInstance(mockAuditService))
            .build()

        running(application) {
          val result = route(application, fakeRequest).value

          status(result)                 shouldBe SEE_OTHER
          redirectLocation(result).value shouldBe "http://grs-start-url"

          verify(mockAuditService, times(1)).auditNewEnrolmentStarted(
            any(),
            any(),
            eqTo(testEnrolmentId),
            eqTo(testGroupId)
          )(any)
        }
      }

      "must not audit EnrolmentStarted(newEnrolment) when user is not new" in {
        val journeyData = JourneyData(
          groupId = testGroupId,
          enrolmentId = testEnrolmentId
        )

        when(mockJourneyAnswersService.getOrCreateEnrolment(any)(any))
          .thenReturn(Future.successful(GetOrCreateEnrolmentResponse(false, journeyData)))

        when(mockGrsService.getGRSJourneyStartUrl(any[HeaderCarrier], any[RequestHeader]))
          .thenReturn(Future.successful("http://grs-start-url"))

        val application =
          applicationBuilder(journeyData = Some(journeyData), inject.bind[AuditService].toInstance(mockAuditService))
            .build()

        running(application) {
          val result = route(application, fakeRequest).value

          status(result)                 shouldBe SEE_OTHER
          redirectLocation(result).value shouldBe "http://grs-start-url"

          verifyNoInteractions(mockAuditService)
        }
      }
    }
  }
}
