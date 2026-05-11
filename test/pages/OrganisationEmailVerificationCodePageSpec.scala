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

package pages

import base.SpecBase
import models.journeydata.OrganisationEmail
import org.scalatest.matchers.should.Matchers.shouldBe
import pages.orgemail.OrganisationEmailVerificationCodePage

class OrganisationEmailVerificationCodePageSpec extends SpecBase {

  "OrganisationEmailVerificationCodePage" - {

    "clearAnswer should set verified to false when currently verified" in {
      val after = OrganisationEmailVerificationCodePage.clearAnswer(verifiedEmail)

      after shouldBe verifiedEmail.copy(verified = Some(false))
    }

    "clearAnswer should leave verified as false when currently unverified" in {
      val after = OrganisationEmailVerificationCodePage.clearAnswer(unverifiedEmail)

      after shouldBe unverifiedEmail.copy(verified = Some(false))
    }

    "clearAnswer should set verified to false when verified flag is missing" in {
      val after = OrganisationEmailVerificationCodePage.clearAnswer(emailWithMissingVerifiedFlag)

      after shouldBe emailWithMissingVerifiedFlag.copy(verified = Some(false))
    }
  }

  private val email: String =
    "test@example.com"

  private def verifiedEmail: OrganisationEmail =
    OrganisationEmail(
      organisationEmail = Some(email),
      verified = Some(true)
    )

  private def unverifiedEmail: OrganisationEmail =
    OrganisationEmail(
      organisationEmail = Some(email),
      verified = Some(false)
    )

  private def emailWithMissingVerifiedFlag: OrganisationEmail =
    OrganisationEmail(
      organisationEmail = Some(email),
      verified = None
    )
}
