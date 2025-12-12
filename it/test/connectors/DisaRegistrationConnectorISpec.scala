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
import play.api.http.Status.{NOT_FOUND, NO_CONTENT, OK, UNAUTHORIZED}
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.http.{JsValidationException, UpstreamErrorResponse}
import utils.BaseIntegrationSpec
import utils.WiremockHelper.{stubGet, stubPost}

class DisaRegistrationConnectorISpec extends BaseIntegrationSpec {

  val testGroupId = "123456"

  val connector: DisaRegistrationConnector = app.injector.instanceOf[DisaRegistrationConnector]

  "DisaRegistrationConnector.getJourneyData" should {

    val getJourneyDataUrl = s"/disa-registration/store/$testGroupId"
    val testIsaProductsAnswers = IsaProducts(Some(IsaProduct.values), None)
    val testJourneyData        = JourneyData(groupId = testGroupId, isaProducts = Some(testIsaProductsAnswers))
    
    "return Some(journeyData) when backend returns 200 OK" in {
      stubGet(getJourneyDataUrl, OK, Json.toJson(testJourneyData).toString)

      val response = await(connector.getJourneyData(testGroupId))
      
      response shouldBe Some(testJourneyData)
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

      val response =
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
}
