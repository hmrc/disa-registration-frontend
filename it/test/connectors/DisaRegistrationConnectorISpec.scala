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

package connectors

import models.journeydata.JourneyData
import models.journeydata.isaproducts.IsaProduct.CashJuniorIsas
import models.journeydata.isaproducts.{IsaProduct, IsaProducts}
import models.submission.EnrolmentSubmissionResponse
import models.GetOrCreateJourneyData
import play.api.http.Status.{CREATED, NOT_FOUND, NO_CONTENT, OK, UNAUTHORIZED}
import play.api.libs.json.JsResultException
import play.api.test.Helpers.await
import uk.gov.hmrc.http.{JsValidationException, UpstreamErrorResponse}
import utils.BaseIntegrationSpec
import utils.WiremockHelper.{stubGet, stubPost}

class DisaRegistrationConnectorISpec extends BaseIntegrationSpec {

  val connector: DisaRegistrationConnector = app.injector.instanceOf[DisaRegistrationConnector]

  val getJourneyDataUrl = s"/disa-registration/store/$testGroupId"
  val getOrCreateEnrolmentUrl = s"/disa-registration/$testGroupId/enrolment"

  val getOrCreateEnrolmentJsonOnCreated =
    s"""
       |{
       |  "groupId": "$testGroupId",
       |  "enrolmentId": "$testEnrolmentId",
       |  "status": "Active"
       |}
       |""".stripMargin

  val expectedGetOrCreateEnrolmentOnCreated =
    GetOrCreateJourneyData(
      isNewEnrolmentJourney = true,
      journeyData = JourneyData(
        groupId = testGroupId,
        enrolmentId = testEnrolmentId
      )
    )

  val journeyDataJson =
    s"""
       |{
       | "groupId": "$testGroupId",
       | "enrolmentId": "$testEnrolmentId",
       | "isaProducts": {
       |   "isaProducts": ["cashIsas", "cashJuniorIsas", "stocksAndSharesIsas", "stocksAndSharesJuniorIsas", "innovativeFinanceIsas"]
       |  }
       |}
       |""".stripMargin

  val expectedJourneyData = JourneyData(groupId = testGroupId, enrolmentId = testEnrolmentId, isaProducts = Some(IsaProducts(Some(IsaProduct.values), None)))

  "DisaRegistrationConnector.getJourneyData" should {
    
    "return Some(journeyData) when backend returns 200 OK" in {
      stubGet(getJourneyDataUrl, OK, journeyDataJson)

      val response = await(connector.getJourneyData(testGroupId))
      
      response shouldBe Some(expectedJourneyData)
    }

    "return None when backend returns 404 Not Found" in {
      stubGet(getJourneyDataUrl, NOT_FOUND, """{"code":"NOT_FOUND", "message":"Not found"}""")

      val response = await(connector.getJourneyData(testGroupId))

      response shouldBe None
    }

    "propagate exception when backend returns an error status (401)" in {
      stubGet(getJourneyDataUrl, UNAUTHORIZED, """{"error":"Not authorised"}""")

      val err = await(connector.getJourneyData(testGroupId).failed)

      err shouldBe an[UpstreamErrorResponse]
      err.getMessage should include ("Not authorised")
    }

    "propagate exception when the call fails with bad json" in {
      stubGet(getJourneyDataUrl, OK, """{"json":"bad"}""")
      val err = await(connector.getJourneyData(testGroupId).failed)

      err shouldBe an[JsValidationException]
    }
  }

  "DisaRegistrationConnector.updateTaskListJourney" should {
    val testSectionAnswers = IsaProducts(Some(Seq(CashJuniorIsas)), None)
    val updateTaskListJourneyUrl = s"/disa-registration/store/$testGroupId/${testSectionAnswers.sectionName}"

    "return Unit when backend returns 204 NoContent" in {
      stubPost(updateTaskListJourneyUrl, NO_CONTENT, "")

      val response: Unit =
        await(connector.updateTaskListJourney(testSectionAnswers, testGroupId, testSectionAnswers.sectionName))

      response shouldBe ()
    }

    "propagate exception when backend returns an error status (401)" in {
      stubPost(updateTaskListJourneyUrl, UNAUTHORIZED, """{"code":"UNAUTHORIZED", "message":"Unauthorised"}""")

      val ex = intercept[UpstreamErrorResponse] {
        await(
          connector.updateTaskListJourney(
            testSectionAnswers,
            testGroupId,
            testSectionAnswers.sectionName
          )
        )
      }

      ex.statusCode shouldBe UNAUTHORIZED
    }

    "propagate exception when the call fails with an unexpected exception" in {
      intercept[UpstreamErrorResponse] {
        await(connector.updateTaskListJourney(testSectionAnswers, testGroupId, testSectionAnswers.sectionName))
      }
    }
  }

  "DisaRegistrationConnector.getOrCreateJourneyData" should {

    "return GetOrCreateJourneyData when backend returns 201" in {
      stubPost(getOrCreateEnrolmentUrl, CREATED, getOrCreateEnrolmentJsonOnCreated)

      val response = await(connector.getOrCreateJourneyData(testGroupId))

      response shouldBe expectedGetOrCreateEnrolmentOnCreated
    }

    "return GetOrCreateJourneyData when backend returns 200" in {
      val getOrCreateEnrolmentJson =
        s"""
           |{
           |  "groupId": "$testGroupId",
           |  "enrolmentId": "$testEnrolmentId",
           |  "status": "Active"
           |}
           |""".stripMargin

      val expectedGetOrCreateEnrolment =
        GetOrCreateJourneyData(
          isNewEnrolmentJourney = false,
          journeyData = JourneyData(
            groupId = testGroupId,
            enrolmentId = testEnrolmentId
          )
        )
      stubPost(getOrCreateEnrolmentUrl, OK, getOrCreateEnrolmentJson)

      val response = await(connector.getOrCreateJourneyData(testGroupId))

      response shouldBe expectedGetOrCreateEnrolment
    }

    "propagate exception when backend returns an error status (401)" in {
      stubPost(getOrCreateEnrolmentUrl, UNAUTHORIZED, """{"code":"UNAUTHORIZED", "message":"Unauthorised"}""")

      val err = await(connector.getOrCreateJourneyData(testGroupId).failed)

      err shouldBe an[UpstreamErrorResponse]
    }

    "propagate exception when the call fails with bad json" in {
      stubPost(getOrCreateEnrolmentUrl, OK, """{"json":"bad"}""")

      val err = await(connector.getOrCreateJourneyData(testGroupId).failed)

      err shouldBe an[JsResultException]
    }
  }


  "DisaRegistrationConnector.declareAndSubmit" should {

    val declareAndSubmitUrl = s"/disa-registration/$testGroupId/declare-and-submit"

    "return EnrolmentSubmissionResponse when backend returns 200 OK" in {
      val responseBody =
        s"""
           | {"receiptId": "$testString"}
           | """.stripMargin
      stubPost(declareAndSubmitUrl, OK, responseBody)

      val response = await(connector.declareAndSubmit(testGroupId))

      response shouldBe EnrolmentSubmissionResponse(testString)
    }

    "propagate exception when backend returns an error status (401)" in {
      stubPost(declareAndSubmitUrl, UNAUTHORIZED, """{"code":"UNAUTHORIZED", "message":"Unauthorised"}""")

      val err = await(connector.declareAndSubmit(testGroupId).failed)

      err shouldBe an[UpstreamErrorResponse]
    }

    "propagate exception when the call fails with bad json" in {
      stubPost(declareAndSubmitUrl, OK, """{"json":"bad"}""")

      val err = await(connector.declareAndSubmit(testGroupId).failed)

      err shouldBe an[JsValidationException]
    }
  }
}
