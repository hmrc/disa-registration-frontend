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

import models.journeyData.isaProducts.IsaProduct.CashJuniorIsas
import models.journeyData.isaProducts.IsaProducts
import play.api.http.Status.{NOT_FOUND, NO_CONTENT, OK, UNAUTHORIZED}
import play.api.test.Helpers.await
import utils.BaseIntegrationSpec
import utils.WiremockHelper.{stubGet, stubPost}

class DisaRegistrationConnectorISpec extends BaseIntegrationSpec {

  val testGroupId = "123456"

  val connector: DisaRegistrationConnector = app.injector.instanceOf[DisaRegistrationConnector]

  "DisaRegistrationConnector.getJourneyData" should {

    val getJourneyDataUrl = s"/disa-registration/store/$testGroupId"

    "return Right(HttpResponse) when backend returns 200 OK" in {
      stubGet(getJourneyDataUrl, OK, "")

      val Right(response) =
        await(connector.getJourneyData(testGroupId).value)

      response.status shouldBe OK
      response.body shouldBe ""
    }

    "return Left(UpstreamErrorResponse) when backend returns an error status (401)" in {
      stubGet(getJourneyDataUrl, UNAUTHORIZED, """{"error":"Not authorised"}""")

      val Left(err) =
        await(connector.getJourneyData(testGroupId).value)

      err.statusCode shouldBe UNAUTHORIZED
      err.message should include("Not authorised")
    }

    "return Left(UpstreamErrorResponse) when the call fails with an unexpected exception" in {
      val Left(err) =
        await(connector.getJourneyData("non-existent").value)

      err.statusCode shouldBe NOT_FOUND
      err.message should include("No response could be served as there are no stub mappings in this WireMock instance.")
    }
  }

  "DisaRegistrationConnector.updateTaskListJourney" should {
    val testSectionAnswers = IsaProducts(Some(Seq(CashJuniorIsas)), None)
    val updateTaskListJourneyUrl = s"/disa-registration/store/$testGroupId/${testSectionAnswers.sectionName}"

    "return Right(HttpResponse) when backend returns 204 NoContent" in {
      stubPost(updateTaskListJourneyUrl, 204, "")

      val Right(response) =
        await(connector.updateTaskListJourney(testSectionAnswers, testGroupId, testSectionAnswers.sectionName).value)

      response.status shouldBe NO_CONTENT
      response.body shouldBe ""
    }

    "return Left(UpstreamErrorResponse) when backend returns an error status (401)" in {
      stubPost(updateTaskListJourneyUrl, UNAUTHORIZED, """{"error":"Not authorised"}""")

      val Left(err) =
        await(connector.updateTaskListJourney(testSectionAnswers, testGroupId, testSectionAnswers.sectionName).value)

      err.statusCode shouldBe UNAUTHORIZED
      err.message should include("Not authorised")
    }

    "return Left(UpstreamErrorResponse) when the call fails with an unexpected exception" in {
      val Left(err) =
        await(connector.updateTaskListJourney(testSectionAnswers, testGroupId, testSectionAnswers.sectionName).value)

      err.statusCode shouldBe NOT_FOUND
      err.message should include("No response could be served as there are no stub mappings in this WireMock instance.")
    }
  }
}
