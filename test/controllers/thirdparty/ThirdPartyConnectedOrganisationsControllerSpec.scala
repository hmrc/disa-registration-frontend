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
import controllers.routes.TaskListController
import controllers.thirdparty.routes.*
import forms.ThirdPartyConnectedOrganisationsFormProvider
import models.NormalMode
import models.YesNoAnswer.Yes
import models.journeydata.thirdparty.{ConnectedThirdPartySelection, ThirdParty, ThirdPartyOrganisations}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import views.html.thirdparty.ThirdPartyConnectedOrganisationsView

import scala.concurrent.Future

class ThirdPartyConnectedOrganisationsControllerSpec extends SpecBase {

  private val routeUrl  = ThirdPartyConnectedOrganisationsController.onPageLoad(NormalMode).url
  private val submitUrl = ThirdPartyConnectedOrganisationsController.onSubmit(NormalMode).url

  val formProvider: ThirdPartyConnectedOrganisationsFormProvider = new ThirdPartyConnectedOrganisationsFormProvider()
  val form: Form[Seq[String]] = formProvider()

  private val tp1 = ThirdParty("1", Some("Org 1"))
  private val tp2 = ThirdParty("2", Some("Org 2"))

  private def journeyData(connected: Seq[String] = Seq.empty) =
    testJourneyData.copy(
      thirdPartyOrganisations = Some(
        ThirdPartyOrganisations(
          managedByThirdParty = Some(Yes),
          thirdParties = Seq(tp1, tp2),
          connectedOrganisations = connected
        )
      )
    )

  "ThirdPartyConnectedOrganisationsController" - {

    "must return OK on GET when section exists" in {

      val application =
        applicationBuilder(journeyData = Some(journeyData(Seq("1")))).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)
        val result  = route(application, request).value

        val view = application.injector.instanceOf[ThirdPartyConnectedOrganisationsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          Seq(tp1, tp2),
          form.fill(Seq("1")),
          NormalMode
        )(request, messages(application)).toString
      }
    }

    "must redirect to task list on GET when section missing" in {

      val application =
        applicationBuilder(journeyData = Some(testJourneyData)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, routeUrl)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect to task list on POST when section missing" in {

      val application =
        applicationBuilder(journeyData = Some(testJourneyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value[]" -> "1")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect to task list on successful POST with selected organisations" in {

      val application =
        applicationBuilder(journeyData = Some(journeyData()))
          .build()

      running(application) {

        when(
          mockJourneyAnswersService.update(
            any(),
            any(),
            any()
          )(any(), any())
        ).thenReturn(Future.successful(journeyData().thirdPartyOrganisations.value))

        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value[]" -> "1", "value[]" -> "2")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must store empty list when 'none' selected and redirect" in {

      val application =
        applicationBuilder(journeyData = Some(journeyData())).build()

      when(
        mockJourneyAnswersService.update(
          any(),
          any(),
          any()
        )(any(), any())
      ).thenReturn(Future.successful(journeyData(Seq.empty).thirdPartyOrganisations.value))

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(
              "value[]" -> ConnectedThirdPartySelection.noneAreConnectedFormValue
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must return BAD_REQUEST when invalid form submitted" in {

      val application =
        applicationBuilder(journeyData = Some(journeyData())).build()

      running(application) {
        val request = FakeRequest(POST, submitUrl)

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }
  }
}