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

package controllers.thirdparty

import base.SpecBase
import controllers.routes.IndexController
import forms.YesNoAnswerFormProvider
import models.YesNoAnswer
import models.journeydata.JourneyData
import models.journeydata.thirdparty.{ThirdParty, ThirdPartyOrganisations}
import models.{CheckMode, NormalMode}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{atMostOnce, verify, when}
import play.api.data.Form
import play.api.libs.json.Writes
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.thirdparty.InvestorFundsUsedByThirdPartyView

import scala.concurrent.Future

class InvestorFundsUsedByThirdPartyControllerSpec extends SpecBase {

  private val existingId = "existing-id"
  private val otherId    = "other-id"
  private val name       = "Test Org"
  private val yesAnswer  = YesNoAnswer.Yes
  private val noAnswer   = YesNoAnswer.No

  val formProvider: YesNoAnswerFormProvider = new YesNoAnswerFormProvider()
  val form: Form[YesNoAnswer]               = formProvider("investorFundsUsedByThirdParty.error.required")

  def onwardRoute =
    routes.InvestorFundsUsedByThirdPartyController.onPageLoad(existingId, NormalMode)

  lazy val routeUrl: String =
    routes.InvestorFundsUsedByThirdPartyController.onPageLoad(existingId, NormalMode).url

  lazy val submitUrl: String =
    routes.InvestorFundsUsedByThirdPartyController.onSubmit(existingId, NormalMode).url

  "InvestorFundsUsedByThirdPartyController" - {

    "GET" - {

      "must return OK when third party exists and no answer yet" in {

        val journeyData =
          JourneyData(
            testGroupId,
            testString,
            thirdPartyOrganisations = Some(
              ThirdPartyOrganisations(
                None,
                Seq(ThirdParty(existingId, Some(name))),
                Set.empty
              )
            )
          )

        val application = applicationBuilder(journeyData = Some(journeyData)).build()

        running(application) {
          val request = FakeRequest(GET, routeUrl)

          val result = route(application, request).value
          val view   = application.injector.instanceOf[InvestorFundsUsedByThirdPartyView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(existingId, name, form, NormalMode)(
            request,
            messages(application)
          ).toString
        }
      }

      "must populate form when previously answered" in {

        val journeyData =
          JourneyData(
            testGroupId,
            testString,
            thirdPartyOrganisations = Some(
              ThirdPartyOrganisations(
                None,
                Seq(
                  ThirdParty(otherId, Some("Other")),
                  ThirdParty(existingId, Some(name), usingInvestorFunds = Some(yesAnswer))
                ),
                Set.empty
              )
            )
          )

        val application = applicationBuilder(journeyData = Some(journeyData)).build()

        running(application) {
          val request = FakeRequest(GET, routeUrl)

          val result = route(application, request).value
          val view   = application.injector.instanceOf[InvestorFundsUsedByThirdPartyView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(existingId, name, form.fill(yesAnswer), NormalMode)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to Index when third party does not exist" in {

        val journeyData =
          JourneyData(
            testGroupId,
            testString,
            thirdPartyOrganisations = Some(
              ThirdPartyOrganisations(None, Seq(ThirdParty(otherId, Some("Other"))), Set.empty)
            )
          )

        val application = applicationBuilder(journeyData = Some(journeyData)).build()

        running(application) {
          val result = route(application, FakeRequest(GET, routeUrl)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual IndexController.onPageLoad().url
        }
      }

      "must redirect to Index when name is missing" in {

        val journeyData =
          JourneyData(
            testGroupId,
            testString,
            thirdPartyOrganisations = Some(
              ThirdPartyOrganisations(None, Seq(ThirdParty(existingId, None)), Set.empty)
            )
          )

        val application = applicationBuilder(journeyData = Some(journeyData)).build()

        running(application) {
          val result = route(application, FakeRequest(GET, routeUrl)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual IndexController.onPageLoad().url
        }
      }
    }

    "POST" - {

      "must return BadRequest when invalid data and third party exists" in {

        val journeyData =
          JourneyData(
            testGroupId,
            testString,
            thirdPartyOrganisations = Some(
              ThirdPartyOrganisations(None, Seq(ThirdParty(existingId, Some(name))), Set.empty)
            )
          )

        val application = applicationBuilder(journeyData = Some(journeyData)).build()

        running(application) {
          val request =
            FakeRequest(POST, submitUrl).withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[InvestorFundsUsedByThirdPartyView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(existingId, name, boundForm, NormalMode)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to Index when invalid data and third party not found" in {

        val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

        running(application) {
          val request =
            FakeRequest(POST, submitUrl).withFormUrlEncodedBody(("value", ""))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual IndexController.onPageLoad().url
        }
      }

      "must update and redirect on valid submission" in {

        val journeyData =
          JourneyData(
            testGroupId,
            testString,
            thirdPartyOrganisations = Some(
              ThirdPartyOrganisations(
                None,
                Seq(
                  ThirdParty(otherId, Some("Other")),
                  ThirdParty(existingId, Some(name))
                ),
                Set.empty
              )
            )
          )

        val expected =
          ThirdPartyOrganisations(
            None,
            Seq(
              ThirdParty(otherId, Some("Other")),
              ThirdParty(existingId, Some(name), usingInvestorFunds = Some(yesAnswer))
            ),
            Set.empty
          )

        when(
          mockJourneyAnswersService
            .update(eqTo(expected), any(), any())(any[Writes[ThirdPartyOrganisations]], any)
        ).thenReturn(Future.successful(expected))

        val application = applicationBuilder(journeyData = Some(journeyData)).build()

        running(application) {
          val request =
            FakeRequest(POST, submitUrl)
              .withFormUrlEncodedBody(("value", yesAnswer.toString))

          val result = route(application, request).value

          verify(mockJourneyAnswersService, atMostOnce)
            .update(eqTo(expected), any(), any())(any[Writes[ThirdPartyOrganisations]], any)

          status(result) mustEqual SEE_OTHER
        }
      }

      "must return Internal Server Error when update fails" in {

        val journeyData =
          JourneyData(
            testGroupId,
            testString,
            thirdPartyOrganisations = Some(
              ThirdPartyOrganisations(
                None,
                Seq(ThirdParty(existingId, Some(name))),
                Set.empty
              )
            )
          )

        when(
          mockJourneyAnswersService
            .update(any[ThirdPartyOrganisations], any(), any())(any(), any())
        ).thenReturn(Future.failed(new Exception("boom")))

        val application = applicationBuilder(journeyData = Some(journeyData)).build()

        running(application) {
          val request =
            FakeRequest(POST, submitUrl)
              .withFormUrlEncodedBody(("value", yesAnswer.toString))

          await(route(application, request).value)

          verify(mockErrorHandler).internalServerError(any[RequestHeader])
        }
      }
    }

    "CheckMode" - {

      "must render GET in CheckMode" in {

        val journeyData =
          JourneyData(
            testGroupId,
            testString,
            thirdPartyOrganisations = Some(
              ThirdPartyOrganisations(
                None,
                Seq(ThirdParty(existingId, Some(name), usingInvestorFunds = Some(noAnswer))),
                Set.empty
              )
            )
          )

        val application = applicationBuilder(journeyData = Some(journeyData)).build()

        running(application) {
          val request =
            FakeRequest(GET, routes.InvestorFundsUsedByThirdPartyController.onPageLoad(existingId, CheckMode).url)

          val result = route(application, request).value
          val view   = application.injector.instanceOf[InvestorFundsUsedByThirdPartyView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(existingId, name, form.fill(noAnswer), CheckMode)(
            request,
            messages(application)
          ).toString
        }
      }
    }
  }
}
