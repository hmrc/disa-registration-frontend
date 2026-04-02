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

package controllers.grs

import base.SpecBase
import models.grs.*
import models.journeydata.{BusinessVerification, RegisteredAddress}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import scala.concurrent.Future

class GrsControllerSpec extends SpecBase {

  val journeyId = "testJourneyId"

  private def fakeRequest =
    FakeRequest(GET, controllers.routes.GrsController.grsCallback(journeyId).url)

  private def baseGRSResponse(
    businessRegistrationStatus: BusinessRegistrationStatus = RegisteredStatus,
    businessVerificationStatus: Option[BusinessVerificationStatus] = Some(BvPass)
  ) =
    GRSResponse(
      companyNumber = "01234567",
      companyName = Some("Test Co"),
      ctutr = Some("1234567890"),
      chrn = None,
      dateOfIncorporation = None,
      countryOfIncorporation = "GB",
      identifiersMatch = true,
      businessRegistrationStatus = businessRegistrationStatus,
      businessVerificationStatus = businessVerificationStatus,
      bpSafeId = Some("X00000123456789"),
      registeredAddress = Some(
        RegisteredAddress(
          addressLine1 = Some("address line 1"),
          addressLine2 = Some("address line 2"),
          addressLine3 = Some("address line 3"),
          postCode = Some("postcode")
        )
      )
    )

  "GrsController" - {

    "grsCallback" - {

      "must redirect to TaskList when both registration and verification pass" in {
        val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()
        val grsResponse = baseGRSResponse()

        when(mockGrsService.fetchGRSJourneyData(eqTo(journeyId))(any()))
          .thenReturn(Future.successful(grsResponse))

        when(mockJourneyAnswersService.update(any[BusinessVerification], any[String], any[String])(any(), any()))
          .thenReturn(
            Future.successful(
              BusinessVerification(
                Some(true),
                Some(true),
                Some("1234567890"),
                companyName = Some("Test Co"),
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

        running(application) {
          val result = route(application, fakeRequest).value

          status(result)                 shouldBe SEE_OTHER
          redirectLocation(result).value shouldBe controllers.routes.TaskListController.onPageLoad().url
        }
      }
      "must redirect to BusinessVerificationLockout when business verification fails and lock the user" in {

        val application = applicationBuilder(journeyData = Some(emptyJourneyData))
          .build()

        val grsResponse = baseGRSResponse(businessVerificationStatus = Some(BvFail))

        when(mockGrsService.fetchGRSJourneyData(eqTo(journeyId))(any()))
          .thenReturn(Future.successful(grsResponse))

        when(mockBvLockoutService.lockUserOut(any[String]))
          .thenReturn(Future.successful(()))

        running(application) {
          val result = route(application, fakeRequest).value

          status(result)                 shouldBe SEE_OTHER
          redirectLocation(result).value shouldBe controllers.routes.BusinessVerificationController.lockout().url

          verify(mockBvLockoutService).lockUserOut(any[String])
        }
      }

      "must redirect to Start when no business registration/verification data present" in {
        val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()
        val grsResponse = baseGRSResponse(
          businessRegistrationStatus = FailedStatus,
          businessVerificationStatus = None
        )

        when(mockGrsService.fetchGRSJourneyData(eqTo(journeyId))(any()))
          .thenReturn(Future.successful(grsResponse))

        when(mockJourneyAnswersService.update(any[BusinessVerification], any[String], any[String])(any(), any()))
          .thenReturn(
            Future.successful(
              BusinessVerification(
                None,
                None,
                Some("1234567890"),
                companyName = Some("Test Co"),
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

        running(application) {
          val result = route(application, fakeRequest).value

          status(result)                 shouldBe SEE_OTHER
          redirectLocation(result).value shouldBe controllers.routes.StartController.onPageLoad().url
        }
      }

      "must redirect to Start when business registration fails" in {
        val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()
        val grsResponse = baseGRSResponse(businessRegistrationStatus = FailedStatus)

        when(mockGrsService.fetchGRSJourneyData(eqTo(journeyId))(any()))
          .thenReturn(Future.successful(grsResponse))

        when(mockJourneyAnswersService.update(any[BusinessVerification], any[String], any[String])(any(), any()))
          .thenReturn(
            Future.successful(
              BusinessVerification(
                Some(false),
                Some(true),
                Some("1234567890"),
                companyName = Some("Test Co"),
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

        running(application) {
          val result = route(application, fakeRequest).value

          status(result)                 shouldBe SEE_OTHER
          redirectLocation(result).value shouldBe controllers.routes.StartController.onPageLoad().url
        }
      }

      "must propagate exception if journeyAnswersService fails" in {
        val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()
        val grsResponse = baseGRSResponse()

        when(mockGrsService.fetchGRSJourneyData(eqTo(journeyId))(any()))
          .thenReturn(Future.successful(grsResponse))

        when(mockJourneyAnswersService.update(any[BusinessVerification], any[String], any[String])(any(), any()))
          .thenReturn(Future.failed(new Exception("Update journeyAnswersService failed - Service Down")))

        running(application) {
          val thrown = route(application, fakeRequest).value.failed.futureValue
          thrown.getMessage shouldBe "Update journeyAnswersService failed - Service Down"
        }
      }

      "must propagate exception if grsService fails" in {
        val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

        when(mockGrsService.fetchGRSJourneyData(eqTo(journeyId))(any()))
          .thenReturn(Future.failed(new Exception("GRS failed - Service Down")))

        running(application) {
          val thrown = route(application, fakeRequest).value.failed.futureValue
          thrown.getMessage shouldBe "GRS failed - Service Down"
        }
      }
    }
  }
}
