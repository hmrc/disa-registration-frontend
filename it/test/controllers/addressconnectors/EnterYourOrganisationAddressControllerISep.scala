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

package controllers.addressconnectors

import models.journeydata.CorrespondenceAddress
import org.jsoup.Jsoup
import play.api.http.Status.*
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, POST, contentAsString, redirectLocation, route, status, writeableOf_AnyContentAsEmpty, writeableOf_AnyContentAsFormUrlEncoded}
import uk.gov.hmrc.http.SessionKeys
import utils.WiremockHelper.{stubGet, stubPost}
import utils.{BaseIntegrationSpec, CommonStubs, WiremockHelper}

class EnterYourOrganisationAddressControllerISep extends BaseIntegrationSpec with CommonStubs with WiremockHelper {

  private val controllerEndpoint = s"$registrationFrontendRoutePrefix/enter-your-organisation-address"
  private val getJourneyDataUrl  = s"/disa-registration/store/$testGroupId"
  private val addressUrl         = "/address-lookup/lookup"
  val testSectionAnswers         =
    CorrespondenceAddress(addressLine1 = Some("1 Test Street"), postCode = Some("AB1 2CD"), uprn = Some("100023336956"))
  val updateTaskListJourneyUrl   = s"/disa-registration/store/$testGroupId/organisationDetails"

  

  "GET /enter-your-organisation-address" should {

    "return 200 and show the correct page" in {
      stubAuth()
      stubGet(getJourneyDataUrl, OK, validGrsJourneyData)

      val request =
        FakeRequest(GET, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get
      val title  = Jsoup.parse(contentAsString(result)).title

      status(result) shouldBe OK
      title            should include("Enter your organisation’s address")
    }

    "return 303 when no Journey data is available" in {
      stubAuth()

      val request =
        FakeRequest(GET, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
    }
  }

  "POST /enter-your-organisation-address" should {

    "return 303 when an address is submitted" in {
      stubAuth()
      // Stub GRS
      stubGet(getJourneyDataUrl, OK, grsRegAddressOrgDetailsJourneyData)
      // Stub Connector
      stubPost(
        addressUrl,
        OK,
        lookUpAddressResponse
      )
      // Stub taskList update
      stubPost(updateTaskListJourneyUrl, NO_CONTENT, "")

      val request =
        FakeRequest(POST, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")
          .withFormUrlEncodedBody("addressLine1" -> "1 Test Street", "postcode" -> "AB1 2CD")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) should not be Some(controllers.routes.GrsStartController.onPageLoad().url)
    }

    "return 500 when an address is submitted" in {
      stubAuth()
      // Stub GRS
      stubGet(getJourneyDataUrl, OK, grsRegAddressOrgDetailsJourneyData)
      // Stub Connector
      stubPost(
        addressUrl,
        SERVICE_UNAVAILABLE,
        ""
      )

      val request =
        FakeRequest(POST, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")
          .withFormUrlEncodedBody("addressLine1" -> "1 Test Street", "postcode" -> "AB1 2CD")

      val result = route(app, request).get

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

  }
}
