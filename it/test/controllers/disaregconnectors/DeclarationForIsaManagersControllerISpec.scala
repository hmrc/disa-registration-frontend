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

package controllers.disaregconnectors

import org.jsoup.Jsoup
import play.api.http.Status.*
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, POST, contentAsString, redirectLocation, route, status, writeableOf_AnyContentAsEmpty}
import uk.gov.hmrc.http.SessionKeys
import utils.WiremockHelper.{stubGet, stubPost}
import utils.{BaseIntegrationSpec, CommonStubs, WiremockHelper}

class DeclarationForIsaManagersControllerISpec extends BaseIntegrationSpec with CommonStubs with WiremockHelper {

  private val controllerEndpoint  = s"$registrationFrontendRoutePrefix/declaration-for-isa-managers"
  private val declareAndSubmitUrl = s"/disa-registration/$testGroupId/declare-and-submit"
  private val getJourneyDataUrl   = s"/disa-registration/store/$testGroupId"

  "GET /declaration-for-isa-managers" should {

    "return 200 with junior content when a junior ISA product was selected" in {
      stubAuth()
      stubGet(getJourneyDataUrl, OK, isaProductsJourneyData)

      val result = route(
        app,
        FakeRequest(GET, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")
      ).get

      val title = Jsoup.parse(contentAsString(result)).title

      status(result) shouldBe OK
      title            should include("Declaration for registering and enrolling ISA managers")
    }

    "303 when there is no ISA products data" in {
      stubAuth()
      stubGet(getJourneyDataUrl, OK, validGrsJourneyData)

      val result = route(
        app,
        FakeRequest(GET, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")
      ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.TaskListController.onPageLoad().url)
    }
  }

  "POST /declaration-for-isa-managers" should {

    "return 303 to the confirmation page when the submission succeeds" in {
      stubAuth()
      // Stub GRS
      stubGet(getJourneyDataUrl, OK, validGrsJourneyData)
      // Stub Connector
      stubPost(declareAndSubmitUrl, OK, """{"formBundleId": "123456789012"}""")

      val request =
        FakeRequest(POST, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.ConfirmationController.onPageLoad().url)
    }

    "return 500 when service is unavailable" in {
      stubAuth()
      // Stub GRS
      stubGet(getJourneyDataUrl, OK, validGrsJourneyData)
      // Stub Connector
      stubPost(declareAndSubmitUrl, SERVICE_UNAVAILABLE, "")

      val request =
        FakeRequest(POST, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

  }

}
