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

package models

import models.journeydata.thirdparty.{ConnectedThirdPartySelection, NoneSelected, SelectedParty}
import models.journeydata.thirdparty.ConnectedThirdPartySelection.noneAreConnectedFormValue
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ConnectedThirdPartySelectionSpec extends AnyWordSpec with Matchers {

  "fromForm" should {

    "return NoneSelected when the 'none are connected' value is present" in {
      val values = Seq(noneAreConnectedFormValue, "party-1", "party-2")

      val result = ConnectedThirdPartySelection.fromForm(values)

      result shouldBe Seq(NoneSelected)
    }

    "return NoneSelected when the 'none are connected' value is the only value" in {
      val values = Seq(noneAreConnectedFormValue)

      val result = ConnectedThirdPartySelection.fromForm(values)

      result shouldBe Seq(NoneSelected)
    }

    "map all values to SelectedParty when 'none are connected' is not present" in {
      val values = Seq("party-1", "party-2")

      val result = ConnectedThirdPartySelection.fromForm(values)

      result shouldBe Seq(
        SelectedParty("party-1"),
        SelectedParty("party-2")
      )
    }

    "return an empty sequence when no values are provided" in {
      val values = Seq.empty[String]

      val result = ConnectedThirdPartySelection.fromForm(values)

      result shouldBe empty
    }
  }
}