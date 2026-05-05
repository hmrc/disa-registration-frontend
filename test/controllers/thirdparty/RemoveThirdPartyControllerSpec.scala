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
import controllers.thirdparty.routes.RemoveThirdPartyController
import forms.YesNoAnswerFormProvider
import models.YesNoAnswer
import models.YesNoAnswer.{No, Yes}
import models.journeydata.thirdparty.{ThirdParty, ThirdPartyOrganisations}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{atMostOnce, verify, when}
import play.api.data.Form
import play.api.libs.json.Writes
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.thirdparty.RemoveThirdPartyView

import scala.concurrent.Future

class RemoveThirdPartyControllerSpec extends SpecBase {

  private val existingId                    = "existing-id"
  private val otherId                       = "other-id"
  private val existingName                  = "Test Org"
  val formProvider: YesNoAnswerFormProvider = new YesNoAnswerFormProvider()
  val form: Form[YesNoAnswer]               = formProvider("removeThirdParty.error.required")

  lazy val routeUrl: String  = RemoveThirdPartyController.onPageLoad(existingId).url
  lazy val submitUrl: String = RemoveThirdPartyController.onSubmit(existingId).url

  val existingThirdParty: ThirdParty =
    ThirdParty(id = existingId, thirdPartyName = Some(existingName))

  val otherThirdParty: ThirdParty =
    ThirdParty(id = otherId, thirdPartyName = Some("Other Org"))

  private def withThirdParties(thirdParties: Seq[ThirdParty]) =
    testJourneyData.copy(
      thirdPartyOrganisations = Some(
        ThirdPartyOrganisations(
          managedByThirdParty = None,
          thirdParties = thirdParties,
          connectedOrganisations = Seq.empty
        )
      )
    )

  "RemoveThirdPartyController" - {

    "must return OK and view when third party exists with name" in {

      val application =
        applicationBuilder(journeyData = Some(withThirdParties(Seq(existingThirdParty)))).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)
        val result  = route(application, request).value

        val view = application.injector.instanceOf[RemoveThirdPartyView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(existingId, existingName, form)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Index when third party does not exist" in {

      val application =
        applicationBuilder(journeyData = Some(withThirdParties(Seq(otherThirdParty)))).build()

      running(application) {
        val result = route(application, FakeRequest(GET, routeUrl)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual IndexController.onPageLoad().url
      }
    }

    "must redirect to Index when name is missing" in {

      val tpWithoutName = ThirdParty(existingId, None)

      val application =
        applicationBuilder(journeyData = Some(withThirdParties(Seq(tpWithoutName)))).build()

      running(application) {
        val result = route(application, FakeRequest(GET, routeUrl)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual IndexController.onPageLoad().url
      }
    }

    "must redirect to Index when section is missing" in {

      val application =
        applicationBuilder(journeyData = Some(testJourneyData)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, routeUrl)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual IndexController.onPageLoad().url
      }
    }

    "must return BadRequest when invalid data submitted" in {

      val application =
        applicationBuilder(journeyData = Some(withThirdParties(Seq(existingThirdParty)))).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> "")

        val view      = application.injector.instanceOf[RemoveThirdPartyView]
        val boundForm = form.bind(Map("value" -> ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(existingId, existingName, boundForm)(
          request,
          messages(application)
        ).toString
      }
    }

    "must remove third party and redirect when Yes is submitted" in {

      val journeyData = withThirdParties(Seq(existingThirdParty, otherThirdParty))

      val expected =
        ThirdPartyOrganisations(
          None,
          Seq(otherThirdParty),
          Seq.empty
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expected), any(), any())(any[Writes[ThirdPartyOrganisations]], any)
      ).thenReturn(Future.successful(expected))

      val application =
        applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> Yes.toString)

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expected), any(), any())(any[Writes[ThirdPartyOrganisations]], any)

        status(result) mustEqual SEE_OTHER
      }
    }

    "must not remove and still update when No is submitted" in {

      val existingSection =
        ThirdPartyOrganisations(None, Seq(existingThirdParty, otherThirdParty), Seq.empty)

      val journeyData =
        testJourneyData.copy(thirdPartyOrganisations = Some(existingSection))

      when(
        mockJourneyAnswersService
          .update(eqTo(existingSection), any(), any())(any[Writes[ThirdPartyOrganisations]], any)
      ).thenReturn(Future.successful(existingSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> No.toString)

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(existingSection), any(), any())(any[Writes[ThirdPartyOrganisations]], any)

        status(result) mustEqual SEE_OTHER
      }
    }

    "must redirect to Index when third party not found on POST" in {

      val application =
        applicationBuilder(journeyData = Some(withThirdParties(Seq(otherThirdParty)))).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> Yes.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual IndexController.onPageLoad().url
      }
    }

    "must redirect to Index when section missing on POST" in {

      val application =
        applicationBuilder(journeyData = Some(testJourneyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> Yes.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual IndexController.onPageLoad().url
      }
    }

    "must return Internal Server Error when update fails" in {

      val journeyData = withThirdParties(Seq(existingThirdParty))

      val updated =
        ThirdPartyOrganisations(None, Seq.empty, Seq.empty)

      when(
        mockJourneyAnswersService
          .update(eqTo(updated), any(), any())(any(), any())
      ).thenReturn(Future.failed(new Exception("boom")))

      val application =
        applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> Yes.toString)

        await(route(application, request).value)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }
  }
}
