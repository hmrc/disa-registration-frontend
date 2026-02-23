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

package controllers.auth

import base.SpecBase
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.auth._

class SignedOutControllerSpec extends SpecBase {

  "SignedOut Controller" - {

    "must return OK and the correct view for a GET for signOut with updates in session" in {

      val application = applicationBuilder(journeyData = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.SignedOutController.signOut(true).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SignOutView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(true)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET for signOut with NO updates in session" in {

      val application = applicationBuilder(journeyData = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.SignedOutController.signOut(false).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SignOutView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(false)(request, messages(application)).toString
      }
    }
  }
}
