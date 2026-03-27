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

package pages

import base.SpecBase
import models.journeydata.{CorrespondenceAddress, OrganisationDetails}
import pages.organisationdetails.RegisteredAddressCorrespondencePage

class RegisteredAddressCorrespondencePageSpec extends SpecBase {

  "RegisteredAddressCorrespondencePage" - {

    "must have the correct string representation" in {
      RegisteredAddressCorrespondencePage.toString mustBe "registeredAddressCorrespondence"
    }

    "clearAnswer" - {

      "must clear registeredAddressCorrespondence when it is set" in {

        val original = OrganisationDetails(
          registeredToManageIsa = Some(true),
          zRefNumber = Some("Z1"),
          tradingUsingDifferentName = Some(true),
          tradingName = Some("Test Ltd"),
          fcaNumber = Some("FCA123"),
          registeredAddressCorrespondence = Some(true),
          correspondenceAddress = Some(
            CorrespondenceAddress(
              addressLine1 = Some("line1"),
              addressLine2 = Some("line2"),
              addressLine3 = Some("line3"),
              postCode = Some("AB12CD")
            )
          ),
          orgTelephoneNumber = Some("123456")
        )

        val result = RegisteredAddressCorrespondencePage.clearAnswer(original)

        result.registeredAddressCorrespondence mustBe None

        result.copy(registeredAddressCorrespondence = Some(true)) mustBe original
      }

      "must remain None if registeredAddressCorrespondence is already None" in {
        val original = OrganisationDetails(
          registeredToManageIsa = Some(true),
          registeredAddressCorrespondence = None
        )
        val result   = RegisteredAddressCorrespondencePage.clearAnswer(original)
        result.registeredAddressCorrespondence mustBe None
        result mustBe original
      }
    }
  }
}
