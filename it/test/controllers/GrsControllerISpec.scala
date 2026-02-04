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

import play.api.http.Status.*
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, redirectLocation, route, status, writeableOf_AnyContentAsEmpty}
import uk.gov.hmrc.http.SessionKeys
import utils.WiremockHelper.{stubGet, stubPost}
import utils.{BaseIntegrationSpec, CommonStubs}

class GrsControllerISpec extends BaseIntegrationSpec with CommonStubs {

  private val journeyId    = "test-journey-id"

  private val callbackUrl =
    s"/obligations/enrolment/isa/incorporated-identity-callback?journeyId=$journeyId"

  private val getJourneyDataUrl =
    s"/disa-registration/store/$testGroupId/businessVerification"

  private val fetchGrsJourneyUrl =
    s"/identify-your-incorporated-business/test-only/retrieve-journey?journeyId=$journeyId"

  "GET /incorporated-identity-callback?journeyId=" should {

    "redirect to TaskList when registration and verification both pass" in {
      val journeyData =
        s"""
           |{
           |  "groupId": "$testGroupId"
           |}
           |""".stripMargin

      val grsResponse =
        """
          |{
          |  "companyProfile": {
          |    "companyName": "Test Company Ltd",
          |    "companyNumber": "01234567"
          |  },
          |  "identifiersMatch": true,
          |  "registration": {
          |    "registrationStatus": "REGISTERED",
          |    "registeredBusinessPartnerId": "X00000123456789"
          |  },
          |  "ctutr": "1234567890",
          |  "businessVerification": {
          |    "verificationStatus": "PASS"
          |  }
          |}
          |""".stripMargin

      stubAuth()
      stubGet(getJourneyDataUrl, OK, journeyData)
      stubGet(fetchGrsJourneyUrl, OK, grsResponse)
      stubPost(getJourneyDataUrl, NO_CONTENT, "")
      val request =
        FakeRequest(GET, callbackUrl)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe
        Some(controllers.routes.TaskListController.onPageLoad().url)
    }

    "redirect to Business Verification lockout when verification fails" in {
      val journeyData =
        s"""
           |{
           |  "groupId": "$testGroupId"
           |}
           |""".stripMargin

      val grsResponse =
        """
          |{
          |  "companyProfile": {
          |    "companyName": "Test Company Ltd",
          |    "companyNumber": "01234567"
          |  },
          |  "identifiersMatch": true,
          |  "registration": {
          |    "registrationStatus": "REGISTERED"
          |  },
          |  "businessVerification": {
          |    "verificationStatus": "FAIL"
          |  }
          |}
          |""".stripMargin

      stubAuth()
      stubGet(getJourneyDataUrl, OK, journeyData)
      stubGet(fetchGrsJourneyUrl, OK, grsResponse)
      stubPost(getJourneyDataUrl, NO_CONTENT, "")

      val request =
        FakeRequest(GET, callbackUrl)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe
        Some(controllers.routes.BusinessVerificationController.lockout().url)
    }

    "redirect to Start when registration has not passed" in {
      val journeyData =
        s"""
           |{
           |  "groupId": "$testGroupId"
           |}
           |""".stripMargin

      val grsResponse =
        """
          |{
          |  "companyProfile": {
          |    "companyName": "Test Company Ltd",
          |    "companyNumber": "01234567"
          |  },
          |  "identifiersMatch": true,
          |  "registration": {
          |    "registrationStatus": "REGISTRATION_FAILED"
          |  }
          |}
          |""".stripMargin

      stubAuth()
      stubGet(getJourneyDataUrl, OK, journeyData)
      stubGet(fetchGrsJourneyUrl, OK, grsResponse)
      stubPost(getJourneyDataUrl, NO_CONTENT, "")

      val request =
        FakeRequest(GET, callbackUrl)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe
        Some(controllers.routes.StartController.onPageLoad().url)
    }

    "redirect to Start when verification has not been attempted" in {
      val journeyData =
        s"""
           |{
           |  "groupId": "$testGroupId"
           |}
           |""".stripMargin

      val grsResponse =
        """
          |{
          |  "companyProfile": {
          |    "companyName": "Test Company Ltd",
          |    "companyNumber": "01234567"
          |  },
          |  "identifiersMatch": true,
          |  "registration": {
          |    "registrationStatus": "REGISTERED"
          |  }
          |}
          |""".stripMargin

      stubAuth()
      stubGet(getJourneyDataUrl, OK, journeyData)
      stubGet(fetchGrsJourneyUrl, OK, grsResponse)
      stubPost(getJourneyDataUrl, NO_CONTENT, "")

      val request =
        FakeRequest(GET, callbackUrl)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe
        Some(controllers.routes.StartController.onPageLoad().url)
    }

  }
}