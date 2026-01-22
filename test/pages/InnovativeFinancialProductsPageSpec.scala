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
import models.journeydata.isaproducts.InnovativeFinancialProduct.PeertopeerLoansUsingAPlatformWith36hPermissions
import models.journeydata.isaproducts.{InnovativeFinancialProduct, IsaProducts}
import org.scalatest.matchers.should.Matchers.shouldBe

class InnovativeFinancialProductsPageSpec extends SpecBase {

  "InnovativeFinancialProductsPage" - {

    "must have the correct string representation" in {
      InnovativeFinancialProductsPage.toString mustBe "innovativeFinancialProducts"
    }

    "clearAnswer should clear innovativeFinancialProducts" in {
      val before = withP2p36h
      val after  = InnovativeFinancialProductsPage.clearAnswer(before)

      after.innovativeFinancialProducts shouldBe None
    }

    "pagesToClear should return P2P dependents when 36H P2P is removed" in {
      val before = withP2p36h
      val after  = withoutP2p36h

      val pages = InnovativeFinancialProductsPage.pagesToClear(before, after)

      pages shouldBe List(PeerToPeerPlatformPage, PeerToPeerPlatformNumberPage)
    }

    "pagesToClear should return Nil when 36H P2P is not removed" in {
      InnovativeFinancialProductsPage.pagesToClear(withP2p36h, withP2p36h)       shouldBe Nil
      InnovativeFinancialProductsPage.pagesToClear(withoutP2p36h, withoutP2p36h) shouldBe Nil
      InnovativeFinancialProductsPage.pagesToClear(withoutP2p36h, withP2p36h)    shouldBe Nil
    }

    "resumeNormalMode should be true only when 36H P2P is added" in {
      InnovativeFinancialProductsPage.resumeNormalMode(withoutP2p36h, withP2p36h) shouldBe true

      InnovativeFinancialProductsPage.resumeNormalMode(withP2p36h, withoutP2p36h)    shouldBe false
      InnovativeFinancialProductsPage.resumeNormalMode(withP2p36h, withP2p36h)       shouldBe false
      InnovativeFinancialProductsPage.resumeNormalMode(withoutP2p36h, withoutP2p36h) shouldBe false
    }
  }

  private val empty: IsaProducts =
    IsaProducts(
      isaProducts = None,
      innovativeFinancialProducts = None,
      p2pPlatform = None,
      p2pPlatformNumber = None
    )

  private def withP2p36h: IsaProducts =
    empty.copy(innovativeFinancialProducts = Some(Seq(PeertopeerLoansUsingAPlatformWith36hPermissions)))

  private def withoutP2p36h: IsaProducts =
    empty.copy(innovativeFinancialProducts =
      Some(Seq(InnovativeFinancialProduct.values.filterNot(_ == PeertopeerLoansUsingAPlatformWith36hPermissions).head))
    )
}
