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
import models.journeydata.isaproducts.IsaProduct.{CashIsas, InnovativeFinanceIsas}
import models.journeydata.isaproducts.IsaProducts
import org.scalatest.matchers.should.Matchers.shouldBe

class IsaProductsPageSpec extends SpecBase {

  "IsaProductsPage" - {

    "must have the correct string representation" in {
      IsaProductsPage.toString mustBe "isaProducts"
    }

    "clearAnswer should clear isaProducts" in {
      val before = withIfIsa
      val after  = IsaProductsPage.clearAnswer(before)

      after.isaProducts shouldBe None
    }

    "pagesToClear should return dependents when IF ISA is removed" in {
      val before = withIfIsa
      val after  = withoutIfIsa

      val pages = IsaProductsPage.pagesToClear(before, after)

      pages shouldBe List(
        InnovativeFinancialProductsPage,
        PeerToPeerPlatformPage,
        PeerToPeerPlatformNumberPage
      )
    }

    "pagesToClear should return Nil when IF ISA is not removed" in {
      IsaProductsPage.pagesToClear(withIfIsa, withIfIsa) shouldBe Nil

      IsaProductsPage.pagesToClear(withoutIfIsa, withoutIfIsa) shouldBe Nil

      IsaProductsPage.pagesToClear(withoutIfIsa, withIfIsa) shouldBe Nil
    }

    "resumeNormalMode should be true only when IF ISA is added" in {
      IsaProductsPage.resumeNormalMode(withoutIfIsa, withIfIsa) shouldBe true

      IsaProductsPage.resumeNormalMode(withIfIsa, withoutIfIsa)    shouldBe false
      IsaProductsPage.resumeNormalMode(withIfIsa, withIfIsa)       shouldBe false
      IsaProductsPage.resumeNormalMode(withoutIfIsa, withoutIfIsa) shouldBe false
    }
  }

  private val empty: IsaProducts =
    IsaProducts(
      isaProducts = None,
      innovativeFinancialProducts = None,
      p2pPlatform = None,
      p2pPlatformNumber = None
    )

  private def withIfIsa: IsaProducts =
    empty.copy(isaProducts = Some(Seq(InnovativeFinanceIsas)))

  private def withoutIfIsa: IsaProducts =
    empty.copy(isaProducts = Some(Seq(CashIsas)))
}
