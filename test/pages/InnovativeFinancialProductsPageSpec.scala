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
      val after = InnovativeFinancialProductsPage.clearAnswer(withP2p36h)

      after.innovativeFinancialProducts shouldBe None
    }

    "pagesToClear should return P2P dependents when 36H P2P is not present and there is an existing dependent answer" in {
      val pages = InnovativeFinancialProductsPage.pagesToClear(withoutP2p36hWithExistingDependentAnswer)

      pages shouldBe List(PeerToPeerPlatformPage, PeerToPeerPlatformNumberPage)
    }

    "pagesToClear should return Nil when there is no stale answers" in {
      InnovativeFinancialProductsPage.pagesToClear(withP2p36hWithExistingDependentAnswer)       shouldBe Nil
      InnovativeFinancialProductsPage.pagesToClear(withP2p36hWithoutExistingDependentAnswer)    shouldBe Nil
      InnovativeFinancialProductsPage.pagesToClear(withoutP2p36hWithoutExistingDependentAnswer) shouldBe Nil
    }

    "resumeNormalMode should be true only when 36H P2P is present and there is no existing dependent answer" in {
      InnovativeFinancialProductsPage.resumeNormalMode(withP2p36hWithoutExistingDependentAnswer) shouldBe true

      InnovativeFinancialProductsPage.resumeNormalMode(withP2p36hWithExistingDependentAnswer) shouldBe false
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

  private def withP2p36hWithExistingDependentAnswer: IsaProducts    = withP2p36h.copy(p2pPlatform = Some(testString))
  private def withP2p36hWithoutExistingDependentAnswer: IsaProducts = withP2p36h.copy(p2pPlatform = None)

  private def withoutP2p36hWithExistingDependentAnswer: IsaProducts    = withoutP2p36h.copy(p2pPlatform = Some(testString))
  private def withoutP2p36hWithoutExistingDependentAnswer: IsaProducts = withoutP2p36h.copy(p2pPlatform = None)
}
