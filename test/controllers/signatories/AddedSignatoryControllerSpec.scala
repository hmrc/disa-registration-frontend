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

package controllers.signatories

import base.SpecBase
import config.FrontendAppConfig
import controllers.routes.TaskListController
import controllers.signatories.routes.*
import models.journeydata.signatories.{Signatories, Signatory}
import models.{NormalMode, YesNoAnswer}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.checkAnswers.signatories.AddedSignatoriesSummary
import views.html.signatories.AddedSignatoryView

class AddedSignatoryControllerSpec extends SpecBase {

  private val routeUrl  = AddedSignatoryController.onPageLoad().url
  private val submitUrl = AddedSignatoryController.onSubmit().url

  private val signatory1 = Signatory("1", Some("Alice"), Some("Dev"))
  private val signatory2 = Signatory("2", Some("Bob"), Some("Dev"))

  private def journeyData(signatories: Seq[Signatory]) =
    testJourneyData.copy(
      signatories = Some(Signatories(signatories))
    )

  "AddedSignatoryController" - {

    "must return OK on GET when signatories exist" in {

      val application =
        applicationBuilder(journeyData = Some(journeyData(Seq(signatory1, signatory2))))
          .build()
      val appConfig   = application.injector.instanceOf[FrontendAppConfig]

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddedSignatoryView]

        val (inProgress, complete) = Seq(signatory1, signatory2).partition(_.inProgress)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form = new forms.YesNoAnswerFormProvider()("addedSignatory.error.required"),
          AddedSignatoriesSummary(inProgress, complete, appConfig.maxSignatories)
        )(request, messages(application)).toString
      }
    }

    "must redirect to task list when no signatories exist" in {

      val application =
        applicationBuilder(journeyData = Some(testJourneyData.copy(signatories = None)))
          .build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect to SignatoryNameController when YES and below max" in {

      val application =
        applicationBuilder(journeyData = Some(journeyData(Seq(signatory1))))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> YesNoAnswer.Yes.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          SignatoryNameController.onPageLoad(None, NormalMode).url
      }
    }

    "must redirect to task list when YES and at max" in {

      val signatories =
        (1 to mockAppConfig.maxSignatories)
          .map(i => Signatory(i.toString, Some("Alice"), Some("Dev")))

      val application =
        applicationBuilder(journeyData = Some(journeyData(signatories)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> YesNoAnswer.Yes.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect to task list when NO selected" in {

      val application =
        applicationBuilder(journeyData = Some(journeyData(Seq(signatory1))))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> YesNoAnswer.No.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must return BAD_REQUEST when form invalid" in {

      val application =
        applicationBuilder(journeyData = Some(journeyData(Seq(signatory1))))
          .build()

      running(application) {
        val request = FakeRequest(POST, submitUrl)

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must redirect to task list when at max and no form value submitted" in {

      val signatories =
        (1 to mockAppConfig.maxSignatories)
          .map(i => Signatory(i.toString, Some("Alice"), Some("Dev")))

      val application =
        applicationBuilder(journeyData = Some(journeyData(signatories)))
          .build()

      running(application) {
        val request = FakeRequest(POST, submitUrl)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect to task list when at max even if invalid form submitted" in {

      val signatories =
        (1 to mockAppConfig.maxSignatories)
          .map(i => Signatory(i.toString, Some("Alice"), Some("Dev")))

      val application =
        applicationBuilder(journeyData = Some(journeyData(signatories)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> "")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must redirect to task list when signatories section is empty" in {

      val application =
        applicationBuilder(journeyData = Some(journeyData(Seq.empty)))
          .build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must return BAD_REQUEST when form invalid and below max" in {

      val application =
        applicationBuilder(journeyData = Some(journeyData(Seq(signatory1))))
          .build()

      running(application) {
        val request = FakeRequest(POST, submitUrl)

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }
    "must NOT return BAD_REQUEST when form invalid and at max" in {

      val signatories =
        (1 to mockAppConfig.maxSignatories)
          .map(i => Signatory(i.toString, Some("Alice"), Some("Dev")))

      val application =
        applicationBuilder(journeyData = Some(journeyData(signatories)))
          .build()

      running(application) {
        val request = FakeRequest(POST, submitUrl) // still invalid (no value)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }
  }
}
