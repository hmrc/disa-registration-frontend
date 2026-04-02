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
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.BusinessVerificationLockoutRepository
import uk.gov.hmrc.http.SessionKeys
import utils.WiremockHelper.{stubGet, stubPost}
import utils.{BaseIntegrationSpec, CommonStubs}

class GrsControllerISpec extends BaseIntegrationSpec with CommonStubs with ScalaFutures {

  private val journeyId = "test-journey-id"

  private val callbackUrl =
    s"/obligations/enrolment/isa/incorporated-identity-callback?journeyId=$journeyId"

  private val getJourneyDataUrl =
    s"/disa-registration/store/$testGroupId/businessVerification"

  private val fetchGrsJourneyUrl =
    s"/incorporated-entity-identification/api/journey/$journeyId"

  private lazy val lockoutRepo =
    app.injector.instanceOf[BusinessVerificationLockoutRepository]

  private val testProviderId = "id"

  private def baseGrsResponse(
                               registrationStatus: String,
                               verificationStatus: Option[String] = None
                             ): String = {

    val bvJson = verificationStatus
      .map(v =>
        s"""
           |,
           |"businessVerification": {
           |  "verificationStatus": "$v"
           |}
           |""".stripMargin
      )
      .getOrElse("")

    s"""
       |{
       |  "companyProfile": {
       |    "companyName": "Test Company Ltd",
       |    "companyNumber": "01234567"
       |  },
       |  "identifiersMatch": true,
       |  "registration": {
       |    "registrationStatus": "$registrationStatus"
       |  }
       |  $bvJson
       |}
       |""".stripMargin
  }

  private def isLockedOut: Boolean =
    await(lockoutRepo.isLockedOut(testProviderId))

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(lockoutRepo.collection.drop().toFuture())
  }

  "GET /incorporated-identity-callback" should {

    "redirect to TaskList when registration AND verification pass" in {

      val journeyData =
        s"""{ "groupId": "$testGroupId" }"""

      val grsResponse =
        baseGrsResponse("REGISTERED", Some("PASS"))

      stubAuth()
      stubGet(getJourneyDataUrl, OK, journeyData)
      stubGet(fetchGrsJourneyUrl, OK, grsResponse)
      stubPost(getJourneyDataUrl, NO_CONTENT, "")

      val result = route(app,
        FakeRequest(GET, callbackUrl)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")
      ).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe
        Some(routes.TaskListController.onPageLoad().url)

      isLockedOut shouldBe false
    }

    "lock user and redirect to lockout when verification FAILS" in {

      val journeyData =
        s"""{ "groupId": "$testGroupId" }"""

      val grsResponse =
        baseGrsResponse("REGISTERED", Some("FAIL"))

      stubAuth()
      stubGet(getJourneyDataUrl, OK, journeyData)
      stubGet(fetchGrsJourneyUrl, OK, grsResponse)
      stubPost(getJourneyDataUrl, NO_CONTENT, "")

      val result = route(app,
        FakeRequest(GET, callbackUrl)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")
      ).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe
        Some(routes.BusinessVerificationController.lockout().url)

      isLockedOut shouldBe true
    }

    "redirect to Start when registration FAILS" in {

      val journeyData =
        s"""{ "groupId": "$testGroupId" }"""

      val grsResponse =
        baseGrsResponse("REGISTRATION_FAILED", Some("PASS"))

      stubAuth()
      stubGet(getJourneyDataUrl, OK, journeyData)
      stubGet(fetchGrsJourneyUrl, OK, grsResponse)
      stubPost(getJourneyDataUrl, NO_CONTENT, "")

      val result = route(app,
        FakeRequest(GET, callbackUrl)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")
      ).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe
        Some(routes.StartController.onPageLoad().url)

      isLockedOut shouldBe false
    }

    "redirect to Start when registration NOT CALLED" in {

      val journeyData =
        s"""{ "groupId": "$testGroupId" }"""

      val grsResponse =
        baseGrsResponse("REGISTRATION_NOT_CALLED", None)

      stubAuth()
      stubGet(getJourneyDataUrl, OK, journeyData)
      stubGet(fetchGrsJourneyUrl, OK, grsResponse)
      stubPost(getJourneyDataUrl, NO_CONTENT, "")

      val result = route(app,
        FakeRequest(GET, callbackUrl)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")
      ).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe
        Some(routes.StartController.onPageLoad().url)

      isLockedOut shouldBe false
    }

    "redirect to Start when verification is missing" in {

      val journeyData =
        s"""{ "groupId": "$testGroupId" }"""

      val grsResponse =
        baseGrsResponse("REGISTERED", None)

      stubAuth()
      stubGet(getJourneyDataUrl, OK, journeyData)
      stubGet(fetchGrsJourneyUrl, OK, grsResponse)
      stubPost(getJourneyDataUrl, NO_CONTENT, "")

      val result = route(app,
        FakeRequest(GET, callbackUrl)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")
      ).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe
        Some(routes.StartController.onPageLoad().url)

      isLockedOut shouldBe false
    }
  }
}