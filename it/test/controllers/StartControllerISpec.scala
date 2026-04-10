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

import org.mongodb.scala.*
import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.BusinessVerificationLockoutRepository
import uk.gov.hmrc.http.SessionKeys
import utils.BaseIntegrationSpec
import utils.WiremockHelper.{stubPost, stubPut}

class StartControllerISpec extends BaseIntegrationSpec with ScalaFutures {

  private val controllerEndpoint = "/obligations/enrolment/isa/start"
  private val getOrCreateEnrolmentUrl = s"/disa-registration/journey/$testGroupId"
  private val grsStartUrl = "/incorporated-entity-identification/api/limited-company-journey"
  
  override lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(config)
      .build()

  private lazy val lockoutRepo =
    app.injector.instanceOf[BusinessVerificationLockoutRepository]

  private def lockUser(): Unit =
    await(lockoutRepo.lockOrg(testGroupId, "1234567890"))

  private def clearLock(): Unit =
    await(lockoutRepo.collection.drop().toFuture())

  "GET /start" should {

    "redirect to TaskList when business verification has passed" in {

      val response =
        s"""
           |{
           |  "isNewEnrolmentJourney": true,
           |  "journeyData": {
           |    "groupId": "$testGroupId",
           |    "enrolmentId": "$testEnrolmentId",
           |    "businessVerification": {
           |      "businessRegistrationPassed": true,
           |      "businessVerificationPassed": true,
           |      "ctutr": "1234567890"
           |    }
           |  }
           |}
           |""".stripMargin

      stubAuth()
      stubPut(getOrCreateEnrolmentUrl, CREATED, response)

      clearLock()

      val request =
        FakeRequest(GET, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.TaskListController.onPageLoad().url)
    }

    "redirect to Business Verification lockout when user is locked out" in {

      val response =
        s"""
           |{
           |  "isNewEnrolmentJourney": true,
           |  "journeyData": {
           |    "groupId": "$testGroupId",
           |    "enrolmentId": "$testEnrolmentId",
           |    "businessVerification": {
           |      "businessRegistrationPassed": true,
           |      "businessVerificationPassed": false,
           |      "ctutr": "1234567890"
           |    }
           |  }
           |}
           |""".stripMargin

      stubAuth()
      stubPut(getOrCreateEnrolmentUrl, CREATED, response)

      lockUser()

      val request =
        FakeRequest(GET, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.BusinessVerificationController.lockout().url)
    }

    "redirect to GRS start URL when user is not locked out and no BV data exists" in {

      val response =
        s"""
           |{
           |  "isNewEnrolmentJourney": false,
           |  "journeyData": {
           |    "groupId": "$testGroupId",
           |    "enrolmentId": "$testEnrolmentId"
           |  }
           |}
           |""".stripMargin

      stubAuth()
      stubPut(getOrCreateEnrolmentUrl, OK, response)

      clearLock()

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

    "redirect to GRS start URL when BV exists but not passed and user NOT locked out" in {

      val response =
        s"""
           |{
           |  "isNewEnrolmentJourney": true,
           |  "journeyData": {
           |    "groupId": "$testGroupId",
           |    "enrolmentId": "$testEnrolmentId",
           |    "businessVerification": {
           |      "businessRegistrationPassed": true,
           |      "businessVerificationPassed": false,
           |      "ctutr": "1234567890"
           |    }
           |  }
           |}
           |""".stripMargin

      stubAuth()
      stubPut(getOrCreateEnrolmentUrl, CREATED, response)

      clearLock()

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

    "redirect to Internal Server Error page when GRS fails" in {

      val response =
        s"""
           |{
           |  "isNewEnrolmentJourney": false,
           |  "journeyData": {
           |    "groupId": "$testGroupId",
           |    "enrolmentId": "$testEnrolmentId"
           |  }
           |}
           |""".stripMargin

      stubAuth()
      stubPut(getOrCreateEnrolmentUrl, OK, response)

      clearLock()

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
      redirectLocation(result) shouldBe Some(
        routes.InternalServerErrorController.onPageLoad().url
      )
    }
  }
}