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

package controllers

import base.SpecBase
import controllers.actions.AuthenticatedIdentifierAction
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.mvc.BodyParsers
import views.html.AddLiaisonOfficerView
import uk.gov.hmrc.auth.core.AffinityGroup

import scala.concurrent.ExecutionContext

class AddLiaisonOfficerControllerSpec extends SpecBase {
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  "AddLiaisonOfficerController" - {

    "must return OK for Organisation users" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

        val authAction = new AuthenticatedIdentifierAction(
          successfulAuthConnector(
            groupId = Some("group-id"),
            affinityGroup = Some(AffinityGroup.Organisation)
          ),
          application.injector.instanceOf[config.FrontendAppConfig],
          bodyParsers
        )

        val controller = new AddLiaisonOfficerController(
          messagesApi = application.injector.instanceOf[play.api.i18n.MessagesApi],
          identify = authAction,
          controllerComponents = application.injector.instanceOf[play.api.mvc.MessagesControllerComponents],
          view = application.injector.instanceOf[AddLiaisonOfficerView]
        )

        val request = FakeRequest(GET, routes.AddLiaisonOfficerController.onPageLoad().url)
        val result  = controller.onPageLoad()(request)

        status(result) mustEqual OK
        contentAsString(result) must include(
          "Add liaison officer - Liaison officers - disa-registration-frontend - GOV.UK"
        ) // optional: check view content
      }
    }

    "must redirect Agent users to Unsupported Affinity Group page" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

        val authAction = new AuthenticatedIdentifierAction(
          successfulAuthConnector(
            groupId = Some("group-id"),
            affinityGroup = Some(AffinityGroup.Agent)
          ),
          application.injector.instanceOf[config.FrontendAppConfig],
          bodyParsers
        )

        val controller = new AddLiaisonOfficerController(
          messagesApi = application.injector.instanceOf[play.api.i18n.MessagesApi],
          identify = authAction,
          controllerComponents = application.injector.instanceOf[play.api.mvc.MessagesControllerComponents],
          view = application.injector.instanceOf[AddLiaisonOfficerView]
        )

        val request = FakeRequest(GET, routes.AddLiaisonOfficerController.onPageLoad().url)
        val result  = controller.onPageLoad()(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.routes.UnsupportedAffinityGroupController
            .onPageLoad(AffinityGroup.Agent.toString)
            .url
      }
    }
  }
}
