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
import models.grs.{BvPass, IncorporatedEntityGRSResponse, NotCalledStatus, PartnershipGRSResponse, RegisteredStatus}
import models.journeydata.RegisteredAddress
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.libs.json.*

import java.time.LocalDate

class GrsResponseSpec extends SpecBase {

  private val testIncorporatedEntityJsonResponse: JsValue = Json.parse(
    """
      |{
      |  "companyProfile": {
      |    "companyName": "Test Company Ltd",
      |    "companyNumber": "01234567",
      |    "dateOfIncorporation": "2020-01-01",
      |    "unsanitisedCHROAddress": {
      |      "address_line_1":"address line 1",
      |      "address_line_2":"address line 2",
      |      "care_of":"test name",
      |      "country":"United Kingdom",
      |      "locality":"test city",
      |      "po_box":"123",
      |      "postal_code":"AA11AA",
      |      "premises":"1",
      |      "region":"test region"
      |    }
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

  "IncorporatedEntityGRSResponse Reads" - {

    "successfully read a full response" in {
      val result = testIncorporatedEntityJsonResponse.validate[IncorporatedEntityGRSResponse]

      result.isSuccess shouldBe true

      val grsResponse = result.get

      grsResponse.companyNumber              shouldBe Some("01234567")
      grsResponse.companyName                shouldBe Some("Test Company Ltd")
      grsResponse.ctutr                      shouldBe Some("1234567890")
      grsResponse.dateOfIncorporation        shouldBe Some(LocalDate.of(2020, 1, 1))
      grsResponse.identifiersMatch           shouldBe true
      grsResponse.businessRegistrationStatus shouldBe RegisteredStatus
      grsResponse.businessVerificationStatus shouldBe Some(BvPass)
      grsResponse.bpSafeId                   shouldBe Some("X00000123456789")
      grsResponse.utr                        shouldBe Some("1234567890")
      grsResponse.registeredAddress          shouldBe Some(
        RegisteredAddress(
          addressLine1 = Some("address line 1"),
          addressLine2 = Some("address line 2"),
          addressLine3 = Some("test city"),
          postCode = Some("AA11AA")
        )
      )
    }

    "successfully read when optional fields are missing" in {
      val minimalJson = Json.parse(
        """
          |{
          |  "identifiersMatch": false,
          |  "registration": {
          |    "registrationStatus": "REGISTERED"
          |  }
          |}
          |""".stripMargin
      )

      val result = minimalJson.validate[IncorporatedEntityGRSResponse]

      result.isSuccess shouldBe true

      val grsResponse = result.get

      grsResponse.companyNumber              shouldBe None
      grsResponse.companyName                shouldBe None
      grsResponse.ctutr                      shouldBe None
      grsResponse.dateOfIncorporation        shouldBe None
      grsResponse.identifiersMatch           shouldBe false
      grsResponse.businessVerificationStatus shouldBe None
      grsResponse.bpSafeId                   shouldBe None
      grsResponse.utr                        shouldBe None
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

      val result = invalidJson.validate[IncorporatedEntityGRSResponse]

      result.isError shouldBe true
    }
  }

  "IncorporatedEntityGRSResponse Writes" - {

    "write a response to JSON" in {
      val grsResponse = IncorporatedEntityGRSResponse(
        companyNumber = Some("01234567"),
        companyName = Some("Test Company Ltd"),
        ctutr = Some("1234567890"),
        dateOfIncorporation = Some(LocalDate.of(2020, 1, 1)),
        identifiersMatch = true,
        businessRegistrationStatus = RegisteredStatus,
        businessVerificationStatus = Some(BvPass),
        bpSafeId = Some("X00000123456789"),
        registeredAddress = Some(
          RegisteredAddress(
            addressLine1 = Some("address line 1"),
            addressLine2 = Some("address line 2"),
            addressLine3 = Some("address line 3"),
            postCode = Some("postcode")
          )
        )
      )

      val json = Json.toJson(grsResponse)

      (json \ "companyNumber").as[String]              shouldBe "01234567"
      (json \ "companyName").as[String]                shouldBe "Test Company Ltd"
      (json \ "ctutr").as[String]                      shouldBe "1234567890"
      (json \ "identifiersMatch").as[Boolean]          shouldBe true
      (json \ "businessRegistrationStatus").as[String] shouldBe "REGISTERED"
      (json \ "businessVerificationStatus").as[String] shouldBe "PASS"
      (json \ "bpSafeId").as[String]                   shouldBe "X00000123456789"
    }
  }

  private val testPartnershipJsonResponse: JsValue = Json.parse(
    """
      |{
      |  "sautr": "1234567890",
      |  "postcode": "AA11AA",
      |  "identifiersMatch": true,
      |  "registration": {
      |    "registrationStatus": "REGISTERED",
      |    "registeredBusinessPartnerId": "X00000123456789"
      |  },
      |  "businessVerification": {
      |    "verificationStatus": "PASS"
      |  }
      |}
      |""".stripMargin
  )

  "PartnershipGRSResponse Reads" - {

    "successfully read a response with no company profile (General/Scottish Partnership)" in {
      val result = testPartnershipJsonResponse.validate[PartnershipGRSResponse]

      result.isSuccess shouldBe true

      val grsResponse = result.get

      grsResponse.sautr                      shouldBe Some("1234567890")
      grsResponse.saPostcode                 shouldBe Some("AA11AA")
      grsResponse.companyNumber              shouldBe None
      grsResponse.identifiersMatch           shouldBe true
      grsResponse.businessRegistrationStatus shouldBe RegisteredStatus
      grsResponse.businessVerificationStatus shouldBe Some(BvPass)
      grsResponse.bpSafeId                   shouldBe Some("X00000123456789")
      grsResponse.utr                        shouldBe Some("1234567890")
    }

    "successfully read a response including a company profile (Limited/Scottish Limited Partnership/LLP)" in {
      val json = Json.parse(
        """
          |{
          |  "sautr": "1234567890",
          |  "postcode": "AA11AA",
          |  "companyProfile": {
          |    "companyName": "Test Company Ltd",
          |    "companyNumber": "01234567",
          |    "dateOfIncorporation": "2020-01-01",
          |    "unsanitisedCHROAddress": {
          |      "postal_code": "AA11AA"
          |    }
          |  },
          |  "identifiersMatch": true,
          |  "registration": {
          |    "registrationStatus": "REGISTERED",
          |    "registeredBusinessPartnerId": "X00000123456789"
          |  }
          |}
          |""".stripMargin
      )

      val result = json.validate[PartnershipGRSResponse]

      result.isSuccess shouldBe true

      val grsResponse = result.get

      grsResponse.sautr                      shouldBe Some("1234567890")
      grsResponse.saPostcode                 shouldBe Some("AA11AA")
      grsResponse.companyNumber              shouldBe Some("01234567")
      grsResponse.companyName                shouldBe Some("Test Company Ltd")
      grsResponse.identifiersMatch           shouldBe true
      grsResponse.businessRegistrationStatus shouldBe RegisteredStatus
      grsResponse.bpSafeId                   shouldBe Some("X00000123456789")
      grsResponse.registeredAddress          shouldBe Some(
        RegisteredAddress(postCode = Some("AA11AA"))
      )
    }

    "successfully read a response with no identifiers collected (no SA UTR path)" in {
      val json = Json.parse(
        """
          |{
          |  "identifiersMatch": false,
          |  "registration": {
          |    "registrationStatus": "REGISTRATION_NOT_CALLED"
          |  }
          |}
          |""".stripMargin
      )

      val result = json.validate[PartnershipGRSResponse]

      result.isSuccess shouldBe true

      val grsResponse = result.get

      grsResponse.sautr                      shouldBe None
      grsResponse.saPostcode                 shouldBe None
      grsResponse.companyNumber              shouldBe None
      grsResponse.companyName                shouldBe None
      grsResponse.identifiersMatch           shouldBe false
      grsResponse.businessRegistrationStatus shouldBe NotCalledStatus
      grsResponse.businessVerificationStatus shouldBe None
      grsResponse.bpSafeId                   shouldBe None
      grsResponse.registeredAddress          shouldBe None
      grsResponse.utr                        shouldBe None
    }

    "fail when mandatory fields are missing" in {
      val invalidJson = Json.parse(
        """
          |{
          |  "sautr": "1234567890"
          |}
          |""".stripMargin
      )

      val result = invalidJson.validate[PartnershipGRSResponse]

      result.isError shouldBe true
    }
  }

  "PartnershipGRSResponse Writes" - {

    "write a full response to JSON" in {
      val grsResponse = PartnershipGRSResponse(
        sautr = Some("1234567890"),
        saPostcode = Some("AA11AA"),
        companyNumber = Some("01234567"),
        companyName = Some("Test Company Ltd"),
        identifiersMatch = true,
        businessRegistrationStatus = RegisteredStatus,
        businessVerificationStatus = Some(BvPass),
        bpSafeId = Some("X00000123456789"),
        registeredAddress = Some(
          RegisteredAddress(
            addressLine1 = Some("address line 1"),
            addressLine2 = Some("address line 2"),
            addressLine3 = Some("address line 3"),
            postCode = Some("postcode")
          )
        )
      )

      val json = Json.toJson(grsResponse)

      (json \ "sautr").as[String]                      shouldBe "1234567890"
      (json \ "saPostcode").as[String]                 shouldBe "AA11AA"
      (json \ "companyNumber").as[String]              shouldBe "01234567"
      (json \ "companyName").as[String]                shouldBe "Test Company Ltd"
      (json \ "identifiersMatch").as[Boolean]          shouldBe true
      (json \ "businessRegistrationStatus").as[String] shouldBe "REGISTERED"
      (json \ "businessVerificationStatus").as[String] shouldBe "PASS"
      (json \ "bpSafeId").as[String]                   shouldBe "X00000123456789"
    }

    "write a minimal response to JSON, omitting optional fields" in {
      val grsResponse = PartnershipGRSResponse(
        identifiersMatch = false,
        businessRegistrationStatus = NotCalledStatus,
        businessVerificationStatus = None,
        bpSafeId = None,
        registeredAddress = None
      )

      val json = Json.toJson(grsResponse)

      (json \ "sautr").toOption                        shouldBe None
      (json \ "saPostcode").toOption                   shouldBe None
      (json \ "companyNumber").toOption                shouldBe None
      (json \ "companyName").toOption                  shouldBe None
      (json \ "identifiersMatch").as[Boolean]          shouldBe false
      (json \ "businessRegistrationStatus").as[String] shouldBe "REGISTRATION_NOT_CALLED"
      (json \ "businessVerificationStatus").toOption   shouldBe None
      (json \ "bpSafeId").toOption                     shouldBe None
      (json \ "registeredAddress").toOption            shouldBe None
    }
  }
}
