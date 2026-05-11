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

package viewmodels.checkAnswers.thirdparty

import base.SpecBase
import models.journeydata.thirdparty.ConnectedThirdPartySelection.noneAreConnectedFormValue
import models.journeydata.thirdparty.ThirdParty
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.ExclusiveCheckbox
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text

class ThirdPartyCheckboxItemsSpec extends SpecBase {

  implicit lazy val msgs: Messages = stubMessages()

  private val tp1      = ThirdParty(id = "id-1", thirdPartyName = Some("Org One"))
  private val tp2      = ThirdParty(id = "id-2", thirdPartyName = Some("Org Two"))
  private val tpNoName = ThirdParty(id = "id-3", thirdPartyName = None)

  "ThirdPartyCheckboxItems.items" - {

    "must create checkbox items for each third party that has a name" in {
      val items =
        ThirdPartyCheckboxItems.items(
          thirdParties = Seq(tp1, tp2),
          selected = Nil
        )

      items.map(_.content) must contain allOf (
        Text("Org One"),
        Text("Org Two")
      )
    }

    "must not create checkbox items for third parties without a name" in {
      val items =
        ThirdPartyCheckboxItems.items(
          thirdParties = Seq(tp1, tpNoName),
          selected = Nil
        )

      items.map(_.content) must contain(Text("Org One"))
      items.map(_.content) must not contain Text("")
    }

    "must mark a third party as checked when its id is selected" in {
      val items =
        ThirdPartyCheckboxItems.items(
          thirdParties = Seq(tp1, tp2),
          selected = Seq("id-2")
        )

      val checkedItems =
        items.filter(_.checked).map(_.content)

      checkedItems mustBe Seq(Text("Org Two"))
    }

    "must include a divider before the 'none are connected' option" in {
      val items =
        ThirdPartyCheckboxItems.items(
          thirdParties = Seq(tp1),
          selected = Nil
        )

      items.exists(_.divider.isDefined) mustBe true
    }

    "must include an exclusive 'None are connected' checkbox" in {
      val items =
        ThirdPartyCheckboxItems.items(
          thirdParties = Seq(tp1),
          selected = Nil
        )

      val noneItem =
        items.find(_.value.contains(noneAreConnectedFormValue)).value

      noneItem.content mustBe Text(
        msgs("thirdPartyConnectedOrganisations.noneSelected")
      )
      noneItem.behaviour mustBe Some(ExclusiveCheckbox)
    }

    "must check the 'None are connected' checkbox when selected" in {
      val items =
        ThirdPartyCheckboxItems.items(
          thirdParties = Seq(tp1, tp2),
          selected = Seq(noneAreConnectedFormValue)
        )

      val noneItem =
        items.find(_.value.contains(noneAreConnectedFormValue)).value

      noneItem.checked mustBe true
    }

    "must ensure 'None are connected' takes precedence over individual selections" in {
      val items =
        ThirdPartyCheckboxItems.items(
          thirdParties = Seq(tp1, tp2),
          selected = Seq("id-1", noneAreConnectedFormValue)
        )

      val checkedItems =
        items.filter(_.checked).map(_.content)

      checkedItems mustBe Seq(
        Text(msgs("thirdPartyConnectedOrganisations.noneSelected"))
      )
    }
  }
}
