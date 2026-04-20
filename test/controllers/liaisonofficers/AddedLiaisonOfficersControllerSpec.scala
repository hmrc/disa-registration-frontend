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

    "must return BadRequest when invalid data is submitted and the count is below the maximum" in {

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

        val view   = application.injector.instanceOf[AddedLiaisonOfficersView]
        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          AddedLiaisonOfficerSummary(
            inProgress = Seq(loInProgress),
            complete = Seq.empty
          )
        )(request, messages(application)).toString
      }
    }

    "must redirect to LiaisonOfficerName when Yes is submitted and the count is below the maximum" in {

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

    "must redirect to TaskList when No is submitted and the count is below the maximum" in {
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

    "must redirect to TaskList when data is submitted and the count is equal to the maximum" in {

      val application = applicationBuilder(Some(emptyJourneyData)).build()

      running(application) {
        val appConfig = application.injector.instanceOf[config.FrontendAppConfig]

        val liaisonOfficers =
          (1 to appConfig.maxLiaisonOfficers).map { i =>
            LiaisonOfficer(
              id = s"id-$i",
              fullName = Some(s"Officer $i"),
              email = Some(s"officer$i@example.com"),
              phoneNumber = Some(s"0700000000$i"),
              communication = Set(ByEmail)
            )
          }

        val journeyData =
          JourneyData(
            groupId = testGroupId,
            enrolmentId = testString,
            liaisonOfficers = Some(LiaisonOfficers(liaisonOfficers))
          )

        val appWithData = applicationBuilder(journeyData = Some(journeyData)).build()

        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> Yes.toString)

        val result = route(appWithData, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect to TaskList when data is submitted and the count is above the maximum" in {
      val appConfig = app.injector.instanceOf[config.FrontendAppConfig]

      val liaisonOfficers =
        (1 to (appConfig.maxLiaisonOfficers + 1)).map { i =>
          LiaisonOfficer(
            id = s"id-$i",
            fullName = Some(s"Officer $i"),
            email = Some(s"officer$i@example.com"),
            phoneNumber = Some(s"0700000000$i"),
            communication = Set(ByEmail)
          )
        }

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(LiaisonOfficers(liaisonOfficers))
        )

      val application = applicationBuilder(Some(journeyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> Yes.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect to TaskList when no liaison officers section exists" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> Yes.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect to TaskList when the liaison officers section is empty" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(LiaisonOfficers(Seq.empty))
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
  }
}
