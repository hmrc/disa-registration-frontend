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

package viewmodels

import models.journeydata.thirdparty.*
import models.journeydata.thirdparty.ConnectedThirdPartySelection.noneAreConnectedFormValue
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.*
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text

object ThirdPartyCheckboxItems {

  def items(
    thirdParties: Seq[ThirdParty],
    selected: Seq[String]
  )(implicit messages: Messages): Seq[CheckboxItem] = {

    val selections = ConnectedThirdPartySelection.fromForm(selected)

    val partyItems: Seq[CheckboxItem] =
      thirdParties.flatMap { tp =>
        tp.thirdPartyName.map { name =>
          CheckboxItem(
            content = Text(name),
            id = Some(s"value-${tp.id}"),
            name = Some(s"value[]"),
            value = name,
            checked = selections.contains(SelectedParty(tp.id))
          )
        }
      }

    val divider: Seq[CheckboxItem] =
      Seq(
        CheckboxItem(
          divider = Some(
            messages("thirdPartyConnectedOrganisations.checkBoxDivider")
          )
        )
      )

    val noneItem: Seq[CheckboxItem] =
      Seq(
        CheckboxItem(
          content = Text(messages("thirdPartyConnectedOrganisations.noneSelected")),
          id = Some("value-none"),
          name = Some("value[]"),
          value = noneAreConnectedFormValue,
          behaviour = Some(ExclusiveCheckbox),
          checked = selections.contains(NoneSelected)
        )
      )

    partyItems ++ divider ++ noneItem
  }
}
