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
import play.api.test.Helpers.{GET, POST, await, contentAsString, route, status, writeableOf_AnyContentAsEmpty, writeableOf_AnyContentAsFormUrlEncoded}
import uk.gov.hmrc.http.SessionKeys
import utils.WiremockHelper.{stubGet, stubPost}
import utils.{BaseIntegrationSpec, CommonStubs, WiremockHelper}

class EmailVerificationCodeControllerISpec extends BaseIntegrationSpec with CommonStubs with WiremockHelper  {

  private val verifyCodePath = "/email-verification/v2/verify-code"
  private val controllerEndpoint = s"$registrationFrontendRoutePrefix/email-verification-code"
  private val getJourneyDataUrl = s"/disa-registration/store/$testGroupId"

  val testSectionAnswers = OrganisationEmail(Some(testEmail), Some(false))
  val updateTaskListJourneyUrl = s"/disa-registration/store/$testGroupId/${testSectionAnswers.sectionName}"

  val emailJourneyData: String =
    s"""
       |{
       |  "groupId": "$testGroupId",
       |  "enrolmentId": "$testString",
       |  "businessVerification": {
       |    "businessRegistrationPassed": true,
       |    "businessVerificationPassed": true,
       |    "ctutr": "1234567890"
       |  },
       |  "organisationEmail": {
       |    "organisationEmail": "$testEmail",
       |    "verified": false
       |  }
       |}
       |""".stripMargin

  "GET /email-verification-code" should {

    "return 200 and show the correct page" in {
      stubAuth()
      stubGet(getJourneyDataUrl, OK, emailJourneyData)


      val request =
        FakeRequest(GET, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get
      val title = Jsoup.parse(contentAsString(result)).title

      status(result) shouldBe OK
      title should include("Code to confirm your organisation email")
    }

    "return 303 when no email is available" in {
      stubAuth()
      stubGet(getJourneyDataUrl, OK, validGrsJourneyData)

      val request =
        FakeRequest(GET, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
    }
  }

  "POST /email-verification-code" should {

    "return 303 when the right code is submitted" in {
      stubAuth()
      //Stub GRS
      stubGet(getJourneyDataUrl, OK, emailJourneyData)
      //Stub Connector
      stubPost(verifyCodePath, OK, """{"result": "verified"}""")
      //Stub taskList update
      stubPost(updateTaskListJourneyUrl, NO_CONTENT, "")

      val form = "value" -> "ABCDEF"

      val request =
        FakeRequest(POST, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")
          .withFormUrlEncodedBody(form)

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
    }

    "return 400 when the wrong code is submitted" in {
      stubAuth()
      //Stub GRS
      stubGet(getJourneyDataUrl, OK, emailJourneyData)
      //Stub Connector
      stubPost(verifyCodePath, OK, """{"result": "verified"}""")
      //Stub taskList update
      stubPost(updateTaskListJourneyUrl, NO_CONTENT, "")

      val form = "value" -> "123456"

      val request =
        FakeRequest(POST, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")
          .withFormUrlEncodedBody(form)

      val result = route(app, request).get

      status(result) shouldBe BAD_REQUEST
    }

    "return 500 when the connector fails" in {
      stubAuth()
      //Stub GRS
      stubGet(getJourneyDataUrl, OK, emailJourneyData)
      //Stub Connector
      stubPost(verifyCodePath, INTERNAL_SERVER_ERROR, "")
      //Stub taskList update
      stubPost(updateTaskListJourneyUrl, NO_CONTENT, "")

      val form = "value" -> "ABCDEF"

      val request =
        FakeRequest(POST, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")
          .withFormUrlEncodedBody(form)

      val result = route(app, request).get

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

  }
}
