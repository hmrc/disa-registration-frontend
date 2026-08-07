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
import org.mockito.Mockito
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import scala.concurrent.Future

class GrsControllerSpec extends SpecBase {

  val journeyId = "testJourneyId"

  private def fakeRequest =
    FakeRequest(GET, controllers.routes.GrsController.grsCallback(journeyId).url)

  private def baseIncorporatedEntityGRSResponse(
    businessRegistrationStatus: BusinessRegistrationStatus = RegisteredStatus,
    businessVerificationStatus: Option[BusinessVerificationStatus] = Some(BvPass),
    ctutr: Option[String] = Some("1234567890")
  ) =
    IncorporatedEntityGRSResponse(
      companyNumber = Some("01234567"),
      companyName = Some("Test Co"),
      ctutr = ctutr,
      dateOfIncorporation = None,
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

  private def basePartnershipGRSResponse(
    businessRegistrationStatus: BusinessRegistrationStatus = RegisteredStatus,
    businessVerificationStatus: Option[BusinessVerificationStatus] = Some(BvPass),
    sautr: Option[String] = Some("1234567890")
  ) =
    PartnershipGRSResponse(
      sautr = sautr,
      saPostcode = Some("AA11AA"),
      companyNumber = None,
      companyName = None,
      identifiersMatch = true,
      businessRegistrationStatus = businessRegistrationStatus,
      businessVerificationStatus = businessVerificationStatus,
      bpSafeId = Some("X00000123456789"),
      registeredAddress = None
    )

  "GrsController" - {

    "grsCallback" - {

      "must redirect to TaskList when both registration and verification pass" in {
        val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()
        val grsResponse = baseIncorporatedEntityGRSResponse()

        when(mockGrsService.fetchGRSJourneyData(eqTo(GrsCompanyType.LimitedCompany), eqTo(journeyId))(any()))
          .thenReturn(Future.successful(grsResponse))

        when(mockJourneyAnswersService.update(any[BusinessVerification], any[String], any[String])(any(), any()))
          .thenReturn(
            Future.successful(
              BusinessVerification(
                Some(true),
                Some(true),
                Some("1234567890"),
                companyName = Some("Test Co"),
                businessPartnerId = Some(testString),
                registeredAddress = Some(
                  RegisteredAddress(
                    addressLine1 = Some("address line 1"),
                    addressLine2 = Some("address line 2"),
                    addressLine3 = Some("address line 3"),
                    postCode = Some("postcode")
                  )
                ),
                companyNumber = Some(testString)
              )
            )
          )

        running(application) {
          val result = route(application, fakeRequest).value

          status(result)                 shouldBe SEE_OTHER
          redirectLocation(result).value shouldBe controllers.routes.TaskListController.onPageLoad().url
        }
      }

      "must redirect to BusinessVerificationController when business verification fails and lock the user when UTR is present" in {

        val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()
        val grsResponse = baseIncorporatedEntityGRSResponse(businessVerificationStatus = Some(BvFail))

        when(mockGrsService.fetchGRSJourneyData(eqTo(GrsCompanyType.LimitedCompany), eqTo(journeyId))(any()))
          .thenReturn(Future.successful(grsResponse))

        when(mockBvLockoutService.lockout(eqTo(testGroupId), eqTo("1234567890")))
          .thenReturn(Future.successful(()))

        running(application) {
          val result = route(application, fakeRequest).value

          status(result)                 shouldBe SEE_OTHER
          redirectLocation(result).value shouldBe controllers.routes.BusinessVerificationController.lockout().url

        }
      }

      "must redirect to BusinessVerificationController and lock the user via SA UTR when a partnership's business verification fails" in {

        val partnershipJourneyData = emptyJourneyData.copy(
          businessVerification = Some(testBV.copy(companyType = Some(GrsCompanyType.GeneralPartnership)))
        )

        val application = applicationBuilder(journeyData = Some(partnershipJourneyData)).build()
        val grsResponse = basePartnershipGRSResponse(businessVerificationStatus = Some(BvFail))

        when(mockGrsService.fetchGRSJourneyData(eqTo(GrsCompanyType.GeneralPartnership), eqTo(journeyId))(any()))
          .thenReturn(Future.successful(grsResponse))

        when(mockBvLockoutService.lockout(eqTo(testGroupId), eqTo("1234567890")))
          .thenReturn(Future.successful(()))

        running(application) {
          val result = route(application, fakeRequest).value

          status(result)                 shouldBe SEE_OTHER
          redirectLocation(result).value shouldBe controllers.routes.BusinessVerificationController.lockout().url

          verify(mockBvLockoutService).lockout(testGroupId, "1234567890")
        }
      }

      "must show error page when business verification fails but UTR is missing" in {

        val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

        val grsResponse =
          baseIncorporatedEntityGRSResponse(
            businessVerificationStatus = Some(BvFail),
            ctutr = None
          )

        when(mockGrsService.fetchGRSJourneyData(eqTo(GrsCompanyType.LimitedCompany), eqTo(journeyId))(any()))
          .thenReturn(Future.successful(grsResponse))

        running(application) {
          val result = route(application, fakeRequest).value

          status(result) shouldBe INTERNAL_SERVER_ERROR
          verify(mockErrorHandler).internalServerError(any)

          verify(mockBvLockoutService, Mockito.never())
            .lockout(any[String], any[String])
        }
      }

      "must show error page when no business registration/verification data present" in {
        val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()
        val grsResponse = baseIncorporatedEntityGRSResponse(
          businessRegistrationStatus = FailedStatus,
          businessVerificationStatus = None
        )

        when(mockGrsService.fetchGRSJourneyData(eqTo(GrsCompanyType.LimitedCompany), eqTo(journeyId))(any()))
          .thenReturn(Future.successful(grsResponse))

        when(mockJourneyAnswersService.update(any[BusinessVerification], any[String], any[String])(any(), any()))
          .thenReturn(
            Future.successful(
              BusinessVerification(
                None,
                None,
                Some("1234567890"),
                companyName = Some("Test Co"),
                businessPartnerId = Some(testString),
                registeredAddress = Some(
                  RegisteredAddress(
                    addressLine1 = Some("address line 1"),
                    addressLine2 = Some("address line 2"),
                    addressLine3 = Some("address line 3"),
                    postCode = Some("postcode")
                  )
                ),
                companyNumber = Some(testString)
              )
            )
          )

        running(application) {
          val result = route(application, fakeRequest).value

          status(result) shouldBe INTERNAL_SERVER_ERROR
          verify(mockErrorHandler).internalServerError(any)
        }
      }

      "must show error page when business registration fails" in {
        val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()
        val grsResponse = baseIncorporatedEntityGRSResponse(businessRegistrationStatus = FailedStatus)

        when(mockGrsService.fetchGRSJourneyData(eqTo(GrsCompanyType.LimitedCompany), eqTo(journeyId))(any()))
          .thenReturn(Future.successful(grsResponse))

        when(mockJourneyAnswersService.update(any[BusinessVerification], any[String], any[String])(any(), any()))
          .thenReturn(
            Future.successful(
              BusinessVerification(
                Some(false),
                Some(true),
                Some("1234567890"),
                companyName = Some("Test Co"),
                businessPartnerId = Some(testString),
                registeredAddress = Some(
                  RegisteredAddress(
                    addressLine1 = Some("address line 1"),
                    addressLine2 = Some("address line 2"),
                    addressLine3 = Some("address line 3"),
                    postCode = Some("postcode")
                  )
                ),
                companyNumber = Some(testString)
              )
            )
          )

        running(application) {
          val result = route(application, fakeRequest).value

          status(result) shouldBe INTERNAL_SERVER_ERROR
          verify(mockErrorHandler).internalServerError(any)
        }
      }

      "must propagate exception if journeyAnswersService fails" in {
        val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()
        val grsResponse = baseIncorporatedEntityGRSResponse()

        when(mockGrsService.fetchGRSJourneyData(eqTo(GrsCompanyType.LimitedCompany), eqTo(journeyId))(any()))
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

        when(mockGrsService.fetchGRSJourneyData(eqTo(GrsCompanyType.LimitedCompany), eqTo(journeyId))(any()))
          .thenReturn(Future.failed(new Exception("GRS failed - Service Down")))

        running(application) {
          val thrown = route(application, fakeRequest).value.failed.futureValue
          thrown.getMessage shouldBe "GRS failed - Service Down"
        }
      }
    }
  }
}
