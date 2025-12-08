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
import org.jsoup.Jsoup
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AffinityGroup
import views.html.{UnauthorisedView, UnsupportedAffinityGroupView}

class UnsupportedAffinityGroupControllerSpec extends SpecBase {

  "UnsupportedAffinityGroupController" - {

    "onPageLoad" - {

      "must render the unsupported affinity group view with the correct affinity group - Agent" in {

        val application = applicationBuilder().build()

        running(application) {

          val controller = new UnsupportedAffinityGroupController(
            controllerComponents = application.injector.instanceOf[MessagesControllerComponents],
            unsupportedAffinityGroupView = application.injector.instanceOf[UnsupportedAffinityGroupView],
            unauthorisedView = application.injector.instanceOf[UnauthorisedView]
          )

          val result = controller.onPageLoad(AffinityGroup.Agent.toString)(FakeRequest())

          status(result) mustBe UNAUTHORIZED

          val document = Jsoup.parse(contentAsString(result))

          document
            .select("div.govuk-body > p")
            .first()
            .text() mustBe "You’ve signed in using an agent services Government Gateway user ID. Only users with an organisation account can register to use this service."
        }
      }

      "must render the unsupported affinity group view with the correct affinity group - Individual" in {

        val application = applicationBuilder().build()

        running(application) {

          val controller = new UnsupportedAffinityGroupController(
            controllerComponents = application.injector.instanceOf[MessagesControllerComponents],
            unsupportedAffinityGroupView = application.injector.instanceOf[UnsupportedAffinityGroupView],
            unauthorisedView = application.injector.instanceOf[UnauthorisedView]
          )

          val result = controller.onPageLoad(AffinityGroup.Individual.toString)(FakeRequest())

          status(result) mustBe UNAUTHORIZED

          val document = Jsoup.parse(contentAsString(result))

          document
            .select("div.govuk-body > p")
            .first()
            .text() mustBe "You’ve signed in with an individual account. Only users with an organisation account can register to use this service."
        }
      }
    }
  }
}
