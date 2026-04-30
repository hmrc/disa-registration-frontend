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
import models.GetOrCreateJourneyData
import models.journeydata.{BusinessVerification, JourneyData, RegisteredAddress}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class StartControllerSpec extends SpecBase {

  private def fakeRequest =
    FakeRequest(GET, controllers.routes.StartController.onPageLoad().url)

  "StartController" - {

    "must redirect to Business Verification lockout when user is locked out and BV not passed" in {

      when(mockBvLockoutService.isGroupLockedOut(any[String]))
        .thenReturn(Future.successful(true))

      when(mockJourneyAnswersService.getOrCreateJourneyData(any)(any))
        .thenReturn(Future.successful(GetOrCreateJourneyData(false, emptyJourneyData)))

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .build()

      running(application) {
        val result = route(application, fakeRequest).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe
          controllers.routes.BusinessVerificationController.lockout().url

        verify(mockBvLockoutService).isGroupLockedOut(any[String])
      }
    }

    "must redirect to TaskList when business verification has passed (regardless of lockout)" in {

      val journeyData = JourneyData(
        groupId = "groupId",
        enrolmentId = "enrolmentId",
        businessVerification = Some(
          BusinessVerification(
            businessRegistrationPassed = Some(true),
            businessVerificationPassed = Some(true),
            ctUtr = Some("1234567890"),
            companyName = Some(testString),
            businessPartnerId = Some(testString),
            registeredAddress = Some(
              RegisteredAddress(
                addressLine1 = Some("address line 1"),
                addressLine2 = Some("address line 2"),
                addressLine3 = Some("address line 3"),
                postCode = Some("postcode")
              )
            )
          )
        )
      )

      when(mockBvLockoutService.isGroupLockedOut(any[String]))
        .thenReturn(Future.successful(true)) // even if locked, should ignore

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val result = route(application, fakeRequest).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe
          controllers.routes.TaskListController.onPageLoad().url
      }
    }

    "must redirect to GRS journey start when BV has not passed and user is not locked out" in {

      val journeyData = JourneyData(
        groupId = "groupId",
        enrolmentId = "enrolmentId",
        businessVerification = Some(
          BusinessVerification(
            businessRegistrationPassed = Some(true),
            businessVerificationPassed = Some(false),
            ctUtr = Some("1234567890"),
            companyName = Some(testString),
            businessPartnerId = Some(testString),
            registeredAddress = Some(
              RegisteredAddress(
                addressLine1 = Some("address line 1"),
                addressLine2 = Some("address line 2"),
                addressLine3 = Some("address line 3"),
                postCode = Some("postcode")
              )
            )
          )
        )
      )

      when(mockBvLockoutService.isGroupLockedOut(any[String]))
        .thenReturn(Future.successful(false))

      when(mockGrsService.getGRSJourneyStartUrl(any[HeaderCarrier], any[RequestHeader]))
        .thenReturn(Future.successful("http://grs-start-url"))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val result = route(application, fakeRequest).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe "http://grs-start-url"

        verify(mockBvLockoutService).isGroupLockedOut(any[String])
      }
    }

    "must redirect to GRS journey start when no business verification exists and user is not locked out" in {

      when(mockBvLockoutService.isGroupLockedOut(any[String]))
        .thenReturn(Future.successful(false))

      when(mockGrsService.getGRSJourneyStartUrl(any[HeaderCarrier], any[RequestHeader]))
        .thenReturn(Future.successful("http://grs-start-url"))

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .build()

      running(application) {
        val result = route(application, fakeRequest).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe "http://grs-start-url"
      }
    }

    "must redirect to Internal Server Error when GRS service fails" in {

      when(mockBvLockoutService.isGroupLockedOut(any[String]))
        .thenReturn(Future.successful(false))

      when(mockGrsService.getGRSJourneyStartUrl(any[HeaderCarrier], any[RequestHeader]))
        .thenReturn(Future.failed(new Exception("GRS down")))

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .build()

      running(application) {
        val result = route(application, fakeRequest).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe
          controllers.routes.InternalServerErrorController.onPageLoad().url
      }
    }
  }
}
