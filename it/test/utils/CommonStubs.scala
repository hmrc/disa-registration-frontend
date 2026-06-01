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

package utils

import play.api.http.Status.{OK, UNAUTHORIZED}
import utils.WiremockHelper.{stubGet, stubPost}

trait CommonStubs extends TestData {

  def stubAuth(): Unit = {
    val body =
      s"""{
         | "groupIdentifier": "$testGroupId",
         | "affinityGroup": "Organisation",
         | "optionalCredentials": {"providerId": "id", "providerType": "GovernmentGateway"},
         | "credentialRole": "user",
         | "allEnrolments": []
         | }""".stripMargin

    stubPost("/auth/authorise", status = OK, responseBody = body)
    stubTaxEnrolmentSubscriptions()
  }

  def stubAuthFail(): Unit = stubPost(url = "/auth/authorise", status = UNAUTHORIZED, responseBody = "{}")

  def stubTaxEnrolmentSubscriptions(
    groupId: String = testGroupId,
    status: Int = OK,
    responseBody: String = "[]"
  ): Unit =
    stubGet(s"/tax-enrolments/groups/$groupId/subscriptions", status = status, body = responseBody)

  val testHeaders: Seq[(String, String)] = Seq("Authorization" -> "Bearer mock-bearer-token")

}
