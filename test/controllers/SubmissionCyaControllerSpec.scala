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
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.Assistant
import viewmodels.checkAnswers.submission.SubmissionCyaViewModel
import views.html.SubmissionCyaView

class SubmissionCyaControllerSpec extends SpecBase {

  "Submission CYA Controller" - {

    "must return OK and the correct view for a GET when the user can submit" in {
      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionCyaController.onPageLoad().url)

        val result = route(application, request).value

        val view      = application.injector.instanceOf[SubmissionCyaView]
        val viewModel = SubmissionCyaViewModel(emptyJourneyData)(messages(application))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(viewModel)(request, messages(application)).toString
      }
    }

    "must redirect to TaskList for a GET if user is assistant" in {
      val application = applicationBuilder(journeyData = Some(emptyJourneyData), credentialRole = Assistant).build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionCyaController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.TaskListController.onPageLoad().url
      }
    }

    "must redirect to JourneyRecoveryController for a GET if no existing data is found" in {
      val application = applicationBuilder(journeyData = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionCyaController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the declaration page for a POST when the user can submit" in {
      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request = FakeRequest(POST, routes.SubmissionCyaController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.DeclarationForIsaManagersController.onPageLoad().url
      }
    }

    "must redirect back to task list for a POST when the user is assistant" in {
      val application = applicationBuilder(journeyData = Some(emptyJourneyData), credentialRole = Assistant).build()

      running(application) {
        val request = FakeRequest(POST, routes.SubmissionCyaController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.TaskListController.onPageLoad().url
      }
    }
  }
}
