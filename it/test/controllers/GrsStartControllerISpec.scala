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
import utils.WiremockHelper.{stubGet, stubPost}

class GrsStartControllerISpec extends BaseIntegrationSpec with ScalaFutures {

  private val controllerEndpoint = "/obligations/enrolment/isa/start"
  private val getJourneyDataUrl  = s"/disa-registration/store/$testGroupId"
  private val grsStartUrl        = "/incorporated-entity-identification/api/limited-company-journey"

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

    "redirect to the organisation is enrolled page when Tax Enrolments has a pending subscription" in {
      val subscriptions =
        s"""
           |[
           |  {
           |    "created": 1482329348256,
           |    "lastModified": 1482329348256,
           |    "serviceName": "HMRC-DISA-ORG",
           |    "identifiers": [],
           |    "callback": "url passed in by the subscriber service",
           |    "state": "PENDING",
           |    "groupIdentifier": "$testGroupId"
           |  }
           |]
           |""".stripMargin

      stubAuth()
      stubTaxEnrolmentSubscriptions(responseBody = subscriptions)

      val request =
        FakeRequest(GET, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        routes.OrganisationIsEnrolledController.onPageLoad(enrolmentInProgress = true).url
      )
    }

    "redirect to select company type when no journey data exists at all" in {

      stubAuth()
      stubGet(getJourneyDataUrl, NOT_FOUND, "")

      clearLock()

      val request =
        FakeRequest(GET, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.GrsCompanyTypeController.onPageLoad().url)
    }

    "redirect to TaskList when business verification has passed" in {

      val response =
        s"""
           |{
           |  "groupId": "$testGroupId",
           |  "enrolmentId": "$testEnrolmentId",
           |  "businessVerification": {
           |    "businessRegistrationPassed": true,
           |    "businessVerificationPassed": true,
           |    "ctutr": "1234567890"
           |  }
           |}
           |""".stripMargin

      stubAuth()
      stubGet(getJourneyDataUrl, OK, response)

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
           |  "groupId": "$testGroupId",
           |  "enrolmentId": "$testEnrolmentId",
           |  "businessVerification": {
           |    "businessRegistrationPassed": true,
           |    "businessVerificationPassed": false,
           |    "ctutr": "1234567890"
           |  }
           |}
           |""".stripMargin

      stubAuth()
      stubGet(getJourneyDataUrl, OK, response)

      lockUser()

      val request =
        FakeRequest(GET, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.BusinessVerificationController.lockout().url)
    }

    "redirect to select company type when BV exists but not passed, user NOT locked out and no company type has been selected" in {

      val response =
        s"""
           |{
           |  "groupId": "$testGroupId",
           |  "enrolmentId": "$testEnrolmentId",
           |  "businessVerification": {
           |    "businessRegistrationPassed": true,
           |    "businessVerificationPassed": false,
           |    "ctutr": "1234567890"
           |  }
           |}
           |""".stripMargin

      stubAuth()
      stubGet(getJourneyDataUrl, OK, response)

      clearLock()

      val request =
        FakeRequest(GET, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.GrsCompanyTypeController.onPageLoad().url)
    }

    "redirect to GRS start URL when BV exists but not passed, user NOT locked out and a company type has been selected" in {

      val response =
        s"""
           |{
           |  "groupId": "$testGroupId",
           |  "enrolmentId": "$testEnrolmentId",
           |  "businessVerification": {
           |    "businessRegistrationPassed": true,
           |    "businessVerificationPassed": false,
           |    "ctutr": "1234567890",
           |    "companyType": "limitedCompany"
           |  }
           |}
           |""".stripMargin

      stubAuth()
      stubGet(getJourneyDataUrl, OK, response)

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
           |  "groupId": "$testGroupId",
           |  "enrolmentId": "$testEnrolmentId",
           |  "businessVerification": {
           |    "companyType": "limitedCompany"
           |  }
           |}
           |""".stripMargin

      stubAuth()
      stubGet(getJourneyDataUrl, OK, response)

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
