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
import config.FrontendAppConfig
import controllers.routes.TaskListController
import controllers.thirdparty.routes.*
import models.YesNoAnswer.Yes
import models.journeydata.thirdparty.{ThirdParty, ThirdPartyOrganisations}
import models.{NormalMode, YesNoAnswer}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.checkAnswers.thirdparty.AddedThirdPartiesSummary
import views.html.thirdparty.AddedThirdPartiesView

class AddedThirdPartiesControllerSpec extends SpecBase {

  private val routeUrl  = AddedThirdPartiesController.onPageLoad().url
  private val submitUrl = AddedThirdPartiesController.onSubmit().url

  private val tp1 = ThirdParty("1", Some("Org 1"))
  private val tp2 = ThirdParty("2", Some("Org 2"))

  private def journeyData(thirdParties: Seq[ThirdParty]) =
    testJourneyData.copy(
      thirdPartyOrganisations = Some(
        ThirdPartyOrganisations(
          managedByThirdParty = Some(Yes),
          thirdParties = thirdParties,
          connectedOrganisations = Seq.empty
        )
      )
    )

  "AddedThirdPartiesController" - {

    "must return OK on GET when third parties exist" in {

      val application =
        applicationBuilder(journeyData = Some(journeyData(Seq(tp1, tp2)))).build()

      val appConfig = application.injector.instanceOf[FrontendAppConfig]

      running(application) {
        val request = FakeRequest(GET, routeUrl)
        val result  = route(application, request).value

        val view = application.injector.instanceOf[AddedThirdPartiesView]

        val (inProgress, complete) = Seq(tp1, tp2).partition(_.inProgress)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form = new forms.YesNoAnswerFormProvider()("addedThirdParties.error.required"),
          AddedThirdPartiesSummary(inProgress, complete, appConfig.maxThirdParties)
        )(request, messages(application)).toString
      }
    }

    "must redirect to task list when section missing" in {

      val application =
        applicationBuilder(journeyData = Some(testJourneyData)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, routeUrl)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect to task list when third parties empty" in {

      val application =
        applicationBuilder(journeyData = Some(journeyData(Seq.empty))).build()

      running(application) {
        val result = route(application, FakeRequest(GET, routeUrl)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect to ThirdPartyConnectedOrganisationsController when Save and Continue and at max third parties" in {

      val maxList =
        (1 to 10)
          .map(i => ThirdParty(i.toString, Some(s"Org $i")))

      val application =
        applicationBuilder(journeyData = Some(journeyData(maxList))).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> "")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual ThirdPartyConnectedOrganisationsController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to ThirdPartiesCheckYourAnswerController when NO selected" in {

      val application =
        applicationBuilder(journeyData = Some(journeyData(Seq(tp1)))).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> YesNoAnswer.No.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must return BAD_REQUEST when invalid form and below max" in {

      val application =
        applicationBuilder(journeyData = Some(journeyData(Seq(tp1)))).build()

      running(application) {
        val request = FakeRequest(POST, submitUrl)

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must redirect to task list when at max and no form value submitted" in {

      val maxList =
        (1 to mockAppConfig.maxThirdParties)
          .map(i => ThirdParty(i.toString, Some(s"Org $i")))

      val application =
        applicationBuilder(journeyData = Some(journeyData(maxList))).build()

      running(application) {
        val request = FakeRequest(POST, submitUrl)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must NOT return BAD_REQUEST when invalid form and at max" in {

      val maxList =
        (1 to mockAppConfig.maxThirdParties)
          .map(i => ThirdParty(i.toString, Some(s"Org $i")))

      val application =
        applicationBuilder(journeyData = Some(journeyData(maxList))).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> "")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect to 'Connected Third Parties' page when NO selected and more than one third party exists" in {

      val application =
        applicationBuilder(journeyData = Some(journeyData(Seq(tp1, tp2)))).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> YesNoAnswer.No.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual ThirdPartyConnectedOrganisationsController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to task list when NO selected and only one third party exists" in {

      val application =
        applicationBuilder(journeyData = Some(journeyData(Seq(tp1)))).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> YesNoAnswer.No.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect to add page when YES selected and multiple but below max" in {

      val application =
        applicationBuilder(journeyData = Some(journeyData(Seq(tp1, tp2)))).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> YesNoAnswer.Yes.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          ThirdPartyOrgDetailsController.onPageLoad(None, NormalMode).url
      }
    }

    "must redirect to task list when section exists but no third parties after filtering" in {

      val application =
        applicationBuilder(journeyData = Some(journeyData(Seq.empty))).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> YesNoAnswer.Yes.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }
  }
}
