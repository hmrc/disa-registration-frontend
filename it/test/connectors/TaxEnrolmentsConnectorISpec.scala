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

package connectors

import models.taxenrolments.TaxEnrolmentSubscription
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.test.Helpers.await
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.BaseIntegrationSpec
import utils.WiremockHelper.stubGet

class TaxEnrolmentsConnectorISpec extends BaseIntegrationSpec {

  private val connector = app.injector.instanceOf[TaxEnrolmentsConnector]

  "TaxEnrolmentsConnector.getSubscriptionsByGroupId" should {

    val getSubscriptionsUrl = s"/tax-enrolments/groups/$testGroupId/subscriptions"

    "return subscriptions when Tax Enrolments returns 200 OK" in {
      val response =
        s"""
           |[
           |  {
           |    "created": 1482329348256,
           |    "lastModified": 1482329348256,
           |    "serviceName": "HMRC-DISA-ORG",
           |    "identifiers": [
           |      {
           |        "key": "ZREF",
           |        "value": "Z0001"
           |      }
           |    ],
           |    "callback": "url passed in by the subscriber service",
           |    "state": "PENDING",
           |    "groupIdentifier": "$testGroupId"
           |  }
           |]
           |""".stripMargin

      stubGet(getSubscriptionsUrl, OK, response)

      val result = await(connector.getSubscriptionsByGroupId(testGroupId))

      result shouldBe Seq(
        TaxEnrolmentSubscription(
          serviceName = "HMRC-DISA-ORG",
          state = "PENDING",
          groupIdentifier = Some(testGroupId)
        )
      )
    }

    "return an empty sequence when Tax Enrolments returns 200 OK with an empty array" in {
      stubGet(getSubscriptionsUrl, OK, "[]")

      val result = await(connector.getSubscriptionsByGroupId(testGroupId))

      result shouldBe Seq.empty
    }

    "throw UpstreamErrorResponse when Tax Enrolments returns an error" in {
      stubGet(getSubscriptionsUrl, BAD_REQUEST, "Bad request from Tax Enrolments")

      val ex = await(connector.getSubscriptionsByGroupId(testGroupId).failed)

      ex shouldBe a[UpstreamErrorResponse]
    }
  }
}
