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

import models.journeydata.OrganisationEmail
import org.jsoup.Jsoup
import play.api.http.Status.*
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, POST, contentAsString, route, status, writeableOf_AnyContentAsEmpty, writeableOf_AnyContentAsFormUrlEncoded}
import uk.gov.hmrc.http.SessionKeys
import utils.WiremockHelper.{stubGet, stubPost}
import utils.{BaseIntegrationSpec, CommonStubs, WiremockHelper}

class OrganisationEmailAddressControllerISpec extends BaseIntegrationSpec with CommonStubs with WiremockHelper {

  private val sendCodePath = "/email-verification/v2/send-code"
  private val controllerEndpoint = s"$registrationFrontendRoutePrefix/organisation-email-address"
  private val getJourneyDataUrl = s"/disa-registration/store/$testGroupId"

  val testSectionAnswers = OrganisationEmail(Some(testEmail), Some(false))
  val updateTaskListJourneyUrl = s"/disa-registration/store/$testGroupId/${testSectionAnswers.sectionName}"


  "GET /organisation-email-address" should {

    "return 200 and show the correct page" in {
      stubAuth()
      stubGet(getJourneyDataUrl, OK, validGrsJourneyData)


      val request =
        FakeRequest(GET, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get
      val title = Jsoup.parse(contentAsString(result)).title

      status(result) shouldBe OK
      title should include("What is the email address of your organisation?")
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
  
  "POST /organisation-email-address" should {

    "return 303 when an unverified email is submitted" in {
      stubAuth()
      //Stub GRS
      stubGet(getJourneyDataUrl, OK, validGrsJourneyData)
      //Stub Connector
      stubPost(sendCodePath, OK, """{"result": "sent"}""")
      //Stub taskList update
      stubPost(updateTaskListJourneyUrl, NO_CONTENT, "")

      val form = "value" -> testEmail

      val request =
        FakeRequest(POST, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")
          .withFormUrlEncodedBody(form)

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
    }

    "return 500" in {
      stubAuth()
      //Stub GRS
      stubGet(getJourneyDataUrl, OK, validGrsJourneyData)
      //Stub Connector
      stubPost(sendCodePath, SERVICE_UNAVAILABLE, """{"result": "Service unavailable"}""")
      //Stub taskList Controller
      stubPost(updateTaskListJourneyUrl, NO_CONTENT, "")

      val form = "value" -> testEmail

      val request =
        FakeRequest(POST, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")
          .withFormUrlEncodedBody(form)

      val result = route(app, request).get

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

}
