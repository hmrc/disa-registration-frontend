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
import models.journeydata.isaproducts.IsaProducts
import org.scalatest.matchers.should.Matchers.shouldBe

class PeerToPeerPlatformPageSpec extends SpecBase {

  "PeerToPeerPlatformPage" - {

    "must have the correct string representation" in {
      PeerToPeerPlatformPage.toString mustBe "peerToPeerPlatform"
    }

    "clearAnswer should clear p2pPlatform" in {
      val before = empty.copy(p2pPlatform = Some("some-platform"))
      val after  = PeerToPeerPlatformPage.clearAnswer(before)

      after.p2pPlatform shouldBe None
    }
  }

  private val empty: IsaProducts =
    IsaProducts(
      isaProducts = None,
      innovativeFinancialProducts = None,
      p2pPlatform = None,
      p2pPlatformNumber = None
    )
}
