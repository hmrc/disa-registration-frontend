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

package pages.orgemail

import base.SpecBase
import models.journeydata.OrganisationEmail
import org.scalatest.matchers.should.Matchers.shouldBe

class OrganisationEmailAddressPageSpec extends SpecBase {

  "OrganisationEmailAddressPage" - {

    "pagesToClear should return organisation email verification code page" in {
      OrganisationEmailAddressPage.pagesToClear(verifiedEmail) shouldBe List(
        OrganisationEmailVerificationCodePage
      )
    }

    "resumeNormalMode should be true when email is not verified" in {
      OrganisationEmailAddressPage.resumeNormalMode(unverifiedEmail) shouldBe true
    }

    "resumeNormalMode should be true when verified flag is missing" in {
      OrganisationEmailAddressPage.resumeNormalMode(emailWithMissingVerifiedFlag) shouldBe true
    }

    "resumeNormalMode should be false when email is already verified" in {
      OrganisationEmailAddressPage.resumeNormalMode(verifiedEmail) shouldBe false
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
