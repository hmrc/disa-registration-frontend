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
import models.YesNoAnswer.{No, Yes}
import models.journeydata.thirdparty.{ThirdParty, ThirdPartyOrganisations}
import org.scalatest.matchers.should.Matchers.shouldBe
import pages.thirdparty.ProductsManagedByThirdPartyPage

class ProductsManagedByThirdPartyPageSpec extends SpecBase {

  "ProductsManagedByThirdPartyPage" - {

    "resumeNormalMode should be true when answered Yes and there are no third party details yet" in {
      ProductsManagedByThirdPartyPage.resumeNormalMode(withYesWithoutExistingDependentAnswer) shouldBe true
    }

    "resumeNormalMode should be false when answered Yes and third party details already exist" in {
      ProductsManagedByThirdPartyPage.resumeNormalMode(withYesAndExistingDependentAnswer) shouldBe false
    }

    "resumeNormalMode should be true when answered No" in {
      ProductsManagedByThirdPartyPage.resumeNormalMode(withNoWithoutExistingDependentAnswer) shouldBe true
      ProductsManagedByThirdPartyPage.resumeNormalMode(withNoAndExistingDependentAnswer)     shouldBe true
    }

    "resumeNormalMode should be true when the answer is missing" in {
      ProductsManagedByThirdPartyPage.resumeNormalMode(empty) shouldBe true
    }

    "pagesToClear should return dependent pages when the answer is No and stale third party details exist" in {
      val pages = ProductsManagedByThirdPartyPage.pagesToClear(withNoAndExistingDependentAnswer)
//TODO update as pages are added
      pages shouldBe List()
    }

    "pagesToClear should return Nil when there are no stale dependent answers" in {
      ProductsManagedByThirdPartyPage.pagesToClear(withYesAndExistingDependentAnswer)     shouldBe Nil
      ProductsManagedByThirdPartyPage.pagesToClear(withYesWithoutExistingDependentAnswer) shouldBe Nil
      ProductsManagedByThirdPartyPage.pagesToClear(withNoWithoutExistingDependentAnswer)  shouldBe Nil
      ProductsManagedByThirdPartyPage.pagesToClear(empty)                                 shouldBe Nil
    }
  }

  private val empty: ThirdPartyOrganisations =
    ThirdPartyOrganisations(
      managedByThirdParty = None,
      thirdParties = Nil
    )

  private def withYesWithoutExistingDependentAnswer: ThirdPartyOrganisations =
    empty.copy(managedByThirdParty = Some(Yes))

  private def withYesAndExistingDependentAnswer: ThirdPartyOrganisations =
    empty.copy(
      managedByThirdParty = Some(Yes),
      thirdParties = Seq(
        ThirdParty(testString)
      )
    )

  private def withNoWithoutExistingDependentAnswer: ThirdPartyOrganisations =
    empty.copy(managedByThirdParty = Some(No))

  private def withNoAndExistingDependentAnswer: ThirdPartyOrganisations =
    empty.copy(
      managedByThirdParty = Some(No),
      thirdParties = Seq(
        ThirdParty(testString)
      )
    )
}
