/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.orgdetails

import base.SpecBase
import forms.YesNoFormProvider
import play.api.Application
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.orgdetails.RegisteredIsaManagerView

class RegisteredIsaManagerSpec extends SpecBase {
  private implicit val app: Application = applicationBuilder().build()
  private lazy val regIsaManagerRoute   = routes.RegisteredIsaManagerController.onPageLoad().url
  private val formProvider              = YesNoFormProvider()("orgDetails.registeredIsaManager.error.missing")

  "GET /registered-isa-manager" - {
    "must return OK and the correct view for a GET" in {
      val request = FakeRequest(GET, regIsaManagerRoute)

      val result = route(app, request).value

      val view = injector.instanceOf[RegisteredIsaManagerView]

      status(result) mustEqual OK

      contentAsString(result) mustEqual
        view(formProvider)(request, messages).toString
    }

    "POST /registered-isa-manager" - {
      // TODO Placeholder test, to be rewitten when wired up
      "must return 404 for a valid submission" in {
        val request = FakeRequest(POST, regIsaManagerRoute).withFormUrlEncodedBody("value" -> "true")

        val result = route(app, request).value

        val view = injector.instanceOf[RegisteredIsaManagerView]

        status(result) mustEqual NOT_FOUND
      }

      "must return 400 with errors for invalid submission" in {
        val request   = FakeRequest(POST, regIsaManagerRoute).withFormUrlEncodedBody("value" -> "")
        val boundForm = formProvider.bind(Map("value" -> ""))

        val result = route(app, request).value

        val view = injector.instanceOf[RegisteredIsaManagerView]

        status(result) mustEqual BAD_REQUEST

        contentAsString(result) mustEqual
          view(boundForm)(request, messages).toString
      }
    }
  }
}
