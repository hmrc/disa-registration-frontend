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

import com.github.tomakehurst.wiremock.client.WireMock.{equalToJson, postRequestedFor, urlEqualTo, verify}
import models.journeydata.isaproducts.IsaProduct
import org.jsoup.Jsoup
import play.api.http.Status.*
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, POST, contentAsString, redirectLocation, route, status, writeableOf_AnyContentAsEmpty, writeableOf_AnyContentAsFormUrlEncoded}
import uk.gov.hmrc.http.SessionKeys
import utils.WiremockHelper.{stubGet, stubPost, stubPut}
import utils.{BaseIntegrationSpec, CommonStubs, WiremockHelper}

class IsaProductsControllerISpec extends BaseIntegrationSpec with CommonStubs with WiremockHelper {

  private val controllerEndpoint = "/obligations/enrolment/isa/isa-products"
  private val getJourneyDataUrl = s"/disa-registration/store/$testGroupId"
  private val updateJourneyUrl = s"/disa-registration/store/$testGroupId/isaProducts"
  private val getOrCreateEnrolmentUrl = s"/disa-registration/journey/$testGroupId"

  "GET /isa-products" should {

    "fetch data from the correct endpoint" in {
      val getOrCreateResponse =
        s"""
          |{
          |  "isNewEnrolmentJourney": true,
          |  "journeyData": {
          |    "groupId": "$testGroupId",
          |    "enrolmentId": "$testString",
          |     "isaProducts": {
          |       "isaProducts": ["cashJuniorIsas", "cashIsas", "stocksAndSharesIsas", "stocksAndSharesJuniorIsas", "innovativeFinanceIsas"]
          |     }
          |  }
          |}
          |""" .stripMargin

      stubAuth()
      stubPut(getOrCreateEnrolmentUrl, OK, getOrCreateResponse)
      // TODO Replace PUT call with below line when ATs begin from /start page DFI-1709
      //stubGet(getJourneyDataUrl, OK, testJourneyData)

      val request =
        FakeRequest(GET, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get

      status(result) shouldBe OK

      val doc = Jsoup.parse(contentAsString(result))

      def isCheckedByValue(v: String): Boolean =
        doc.select(s"""input.govuk-checkboxes__input[type=checkbox][value="$v"]""").hasAttr("checked")

      IsaProduct.values.foreach { v =>
        withClue(s"Expected checkbox '$v' to be checked") {
          isCheckedByValue(v.toString) shouldBe true
        }
      }
    }
  }

  "POST /isa-products" should {

    "call correct endpoint with correct data shape" in {
      stubAuth()
      stubGet(getJourneyDataUrl, NOT_FOUND, """{"code":"NOT_FOUND", "message":"Not found"}""")
      stubPost(updateJourneyUrl, NO_CONTENT, "")

      val form = "value[0]" -> "cashJuniorIsas"

      val request =
        FakeRequest(POST, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")
          .withFormUrlEncodedBody(form)

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/obligations/enrolment/isa/isa-products-check-your-answers")

      verify(
        postRequestedFor(urlEqualTo(updateJourneyUrl))
          .withRequestBody(equalToJson(
            """{
              |  "isaProducts": ["cashJuniorIsas"]
              |}""".stripMargin
          ))
      )
    }
  }
}
