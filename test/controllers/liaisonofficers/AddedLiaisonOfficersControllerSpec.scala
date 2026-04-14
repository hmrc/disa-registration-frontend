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

package controllers.liaisonofficers

import base.SpecBase
import controllers.liaisonofficers.routes.LiaisonOfficerNameController
import controllers.routes.TaskListController
import forms.YesNoAnswerFormProvider
import models.NormalMode
import models.YesNoAnswer.{No, Yes}
import models.journeydata.JourneyData
import models.journeydata.liaisonofficers.LiaisonOfficerCommunication.ByEmail
import models.journeydata.liaisonofficers.{LiaisonOfficer, LiaisonOfficers}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.checkAnswers.liaisonofficers.AddedLiaisonOfficerSummary
import views.html.liaisonofficers.AddedLiaisonOfficersView

class AddedLiaisonOfficersControllerSpec extends SpecBase {

  private val formProvider = new YesNoAnswerFormProvider()
  private val form         = formProvider("addedLiaisonOfficers.error.required")

  private val routeUrl  = routes.AddedLiaisonOfficersController.onPageLoad().url
  private val submitUrl = routes.AddedLiaisonOfficersController.onSubmit().url

  private val loInProgress = LiaisonOfficer(id = "1", fullName = Some("Jane Smith"))
  private val loComplete   = LiaisonOfficer(id = "2", fullName = Some("John Doe"), Some(""), Set(ByEmail), Some(""))

  "AddedLiaisonOfficersController" - {

    "must return OK and the correct view on GET when data exists" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(LiaisonOfficers(Seq(loInProgress, loComplete)))
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)
        val result  = route(application, request).value

        val view = application.injector.instanceOf[AddedLiaisonOfficersView]

        val (inProgress, complete) = Seq(loInProgress, loComplete).partition(_.inProgress)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          AddedLiaisonOfficerSummary(inProgress, complete)
        )(request, messages(application)).toString
      }
    }

    "must redirect to TaskList on GET when no liaison officers" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(LiaisonOfficers(Seq.empty))
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, routeUrl)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect to TaskList on GET when section missing" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, routeUrl)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must return BadRequest when POST with invalid data" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(LiaisonOfficers(Seq(loInProgress)))
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> "")

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[AddedLiaisonOfficersView]

        val result = route(application, request).value

        val (inProgress, complete) = Seq(loInProgress).partition(_.inProgress)

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          AddedLiaisonOfficerSummary(inProgress, complete)
        )(request, messages(application)).toString
      }
    }

    "must redirect to liaison name page when Yes and below max" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(LiaisonOfficers(Seq(loInProgress)))
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> Yes.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          LiaisonOfficerNameController.onPageLoad(None, NormalMode).url
      }
    }

    "must redirect to task list when Yes and at max" in {
      val los = (1 to mockAppConfig.maxLiaisonOfficers).map(i => loComplete.copy(id = i.toString))

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(LiaisonOfficers(los))
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> Yes.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect to task list when No" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(LiaisonOfficers(Seq(loInProgress)))
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> No.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }
  }
}
