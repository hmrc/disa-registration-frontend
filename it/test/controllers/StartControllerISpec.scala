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
import utils.WiremockHelper.stubPost
import utils.{BaseIntegrationSpec, CommonStubs}

class StartControllerISpec extends BaseIntegrationSpec with CommonStubs {

  private val controllerEndpoint = "/obligations/enrolment/isa/start"
  private val getOrCreateEnrolmentUrl = s"/disa-registration/$testGroupId/enrolment"
  private val grsStartUrl = "/incorporated-entity-identification/api/limited-company-journey"

  "GET /start" should {

    "redirect to TaskList when business verification has passed" in {
      val journeyData =
        s"""
           |{
           |    "groupId": "$testGroupId",
           |    "enrolmentId": "$testEnrolmentId",
           |    "businessVerification": {
           |      "businessRegistrationPassed": true,
           |      "businessVerificationPassed": true,
           |      "ctutr": "1234567890"
           |    }
           |}
           |""".stripMargin

      stubAuth()
      stubPost(getOrCreateEnrolmentUrl, CREATED, journeyData)

      val request =
        FakeRequest(GET, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.TaskListController.onPageLoad().url)
    }

    "redirect to Business Verification lockout when verification has failed" in {
      val journeyData =
        s"""
           |{
           |    "groupId": "$testGroupId",
           |    "enrolmentId": "$testEnrolmentId",
           |    "businessVerification": {
           |      "businessRegistrationPassed": true,
           |      "businessVerificationPassed": false,
           |      "ctutr": "1234567890"
           |    }
           |}
           |""".stripMargin

      stubAuth()
      stubPost(getOrCreateEnrolmentUrl, CREATED, journeyData)

      val request =
        FakeRequest(GET, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.BusinessVerificationController.lockout().url)
    }

    "redirect to GRS start URL when no business verification data exists" in {
      val journeyData =
        s"""
           |{
           |    "groupId": "$testGroupId",
           |    "enrolmentId": "$testEnrolmentId"
           |}
           |""".stripMargin

      stubAuth()
      stubPost(getOrCreateEnrolmentUrl, OK, journeyData)
      stubPost(
        grsStartUrl,
        OK,
        """{ "journeyStartUrl": "http://localhost:9999/grs/start" }"""
      )

      val request =
        FakeRequest(GET, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("http://localhost:9999/grs/start")
    }

    "redirect to Internal Server Error page when GRS service fails" in {
      val journeyData =
        s"""
           |{
           |    "groupId": "$testGroupId",
           |    "enrolmentId": "$testEnrolmentId"
           |}
           |""".stripMargin

      stubAuth()
      stubPost(getOrCreateEnrolmentUrl, OK, journeyData)
      stubPost(
        grsStartUrl,
        INTERNAL_SERVER_ERROR,
        """{"error":"GRS unavailable"}"""
      )

      val request =
        FakeRequest(GET, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.InternalServerErrorController.onPageLoad().url)
    }
  }
}
