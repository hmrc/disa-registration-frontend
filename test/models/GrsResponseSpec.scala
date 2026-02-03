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

package models

import base.SpecBase
import models.grs.{BvPass, GRSResponse, RegisteredStatus}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.libs.json.*

import java.time.LocalDate


class GrsResponseSpec extends SpecBase {

    private val testGRSJsonResponse: JsValue = Json.parse(
      """
        |{
        |  "companyProfile": {
        |    "companyName": "Test Company Ltd",
        |    "companyNumber": "01234567",
        |    "dateOfIncorporation": "2020-01-01"
        |  },
        |  "identifiersMatch": true,
        |  "registration": {
        |    "registrationStatus": "REGISTERED",
        |    "registeredBusinessPartnerId": "X00000123456789"
        |  },
        |  "ctutr": "1234567890",
        |  "businessVerification": {
        |    "verificationStatus": "PASS"
        |  }
        |}
        |""".stripMargin
    )

    "GRSResponse Reads" - {

      "successfully read a full GRS response" in {
        val result = testGRSJsonResponse.validate[GRSResponse]

        result.isSuccess shouldBe true

        val grsResponse = result.get

        grsResponse.companyNumber shouldBe "01234567"
        grsResponse.companyName shouldBe Some("Test Company Ltd")
        grsResponse.ctutr shouldBe Some("1234567890")
        grsResponse.chrn shouldBe None
        grsResponse.dateOfIncorporation shouldBe Some(LocalDate.of(2020, 1, 1))
        grsResponse.countryOfIncorporation shouldBe "GB"
        grsResponse.identifiersMatch shouldBe true
        grsResponse.businessRegistrationStatus shouldBe RegisteredStatus
        grsResponse.businessVerificationStatus shouldBe Some(BvPass)
        grsResponse.bpSafeId shouldBe Some("X00000123456789")
      }

      "successfully read when optional fields are missing" in {
        val minimalJson = Json.parse(
          """
            |{
            |  "companyProfile": {
            |    "companyNumber": "01234567"
            |  },
            |  "identifiersMatch": false,
            |  "registration": {
            |    "registrationStatus": "REGISTERED"
            |  }
            |}
            |""".stripMargin
        )

        val result = minimalJson.validate[GRSResponse]

        result.isSuccess shouldBe true

        val grsResponse = result.get

        grsResponse.companyNumber shouldBe "01234567"
        grsResponse.companyName shouldBe None
        grsResponse.ctutr shouldBe None
        grsResponse.dateOfIncorporation shouldBe None
        grsResponse.identifiersMatch shouldBe false
        grsResponse.businessVerificationStatus shouldBe None
        grsResponse.bpSafeId shouldBe None
      }

      "fail when mandatory fields are missing" in {
        val invalidJson = Json.parse(
          """
            |{
            |  "companyProfile": {
            |    "companyName": "Test Company Ltd"
            |  }
            |}
            |""".stripMargin
        )

        val result = invalidJson.validate[GRSResponse]

        result.isError shouldBe true
      }
    }

    "GRSResponse Writes" - {

      "write a GRSResponse to JSON" in {
        val grsResponse = GRSResponse(
          companyNumber = "01234567",
          companyName = Some("Test Company Ltd"),
          ctutr = Some("1234567890"),
          chrn = None,
          dateOfIncorporation = Some(LocalDate.of(2020, 1, 1)),
          countryOfIncorporation = "GB",
          identifiersMatch = true,
          businessRegistrationStatus = RegisteredStatus,
          businessVerificationStatus = Some(BvPass),
          bpSafeId = Some("X00000123456789")
        )

        val json = Json.toJson(grsResponse)

        (json \ "companyNumber").as[String] shouldBe "01234567"
        (json \ "companyName").as[String] shouldBe "Test Company Ltd"
        (json \ "ctutr").as[String] shouldBe "1234567890"
        (json \ "identifiersMatch").as[Boolean] shouldBe true
        (json \ "businessRegistrationStatus").as[String] shouldBe "REGISTERED"
        (json \ "businessVerificationStatus").as[String] shouldBe "PASS"
        (json \ "bpSafeId").as[String] shouldBe "X00000123456789"
      }
    }
}
