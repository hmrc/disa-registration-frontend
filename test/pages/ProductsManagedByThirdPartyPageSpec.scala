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
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import pages.thirdparty.ProductsManagedByThirdPartyPage

class ProductsManagedByThirdPartyPageSpec extends SpecBase {

  "ProductsManagedByThirdPartyPage" - {

    "resumeNormalMode should be true when answer is Yes and no third party details exist" in {
      ProductsManagedByThirdPartyPage.resumeNormalMode(withYesWithoutExistingDependentAnswer) shouldBe true
    }

    "resumeNormalMode should be false when answer is Yes and third party details exist" in {
      ProductsManagedByThirdPartyPage.resumeNormalMode(withYesAndExistingDependentAnswer) shouldBe false
    }

    "resumeNormalMode should be true when answer is No (regardless of data existence)" in {
      ProductsManagedByThirdPartyPage.resumeNormalMode(withNoWithoutExistingDependentAnswer) shouldBe true
      ProductsManagedByThirdPartyPage.resumeNormalMode(withNoAndExistingDependentAnswer)     shouldBe true
    }

    "resumeNormalMode should be true when answer is missing" in {
      ProductsManagedByThirdPartyPage.resumeNormalMode(empty) shouldBe true
    }

    "pagesToClear should clear third party details when answer is No and data exists" in {
      val pages = ProductsManagedByThirdPartyPage.pagesToClear(withNoAndExistingDependentAnswer)

      pages should have size 1

      val result = pages.foldLeft(withNoAndExistingDependentAnswer) { case (acc, page) =>
        page.clearAnswer(acc)
      }

      result.thirdParties shouldBe Nil
    }

    "pagesToClear should return Nil when answer is No but no third party details exist" in {
      ProductsManagedByThirdPartyPage.pagesToClear(withNoWithoutExistingDependentAnswer) shouldBe Nil
    }

    "pagesToClear should return Nil when answer is Yes and third party details exist" in {
      ProductsManagedByThirdPartyPage.pagesToClear(withYesAndExistingDependentAnswer) shouldBe Nil
    }

    "pagesToClear should return Nil when answer is Yes and no third party details exist" in {
      ProductsManagedByThirdPartyPage.pagesToClear(withYesWithoutExistingDependentAnswer) shouldBe Nil
    }

    "pagesToClear should return Nil when answer is missing" in {
      ProductsManagedByThirdPartyPage.pagesToClear(empty) shouldBe Nil
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
      thirdParties = Seq(ThirdParty(testString))
    )

  private def withNoWithoutExistingDependentAnswer: ThirdPartyOrganisations =
    empty.copy(managedByThirdParty = Some(No))

  private def withNoAndExistingDependentAnswer: ThirdPartyOrganisations =
    empty.copy(
      managedByThirdParty = Some(No),
      thirdParties = Seq(ThirdParty(testString))
    )
}
