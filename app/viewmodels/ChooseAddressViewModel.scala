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

package viewmodels.address

import models.addresslookup.LookupAddress
import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.html.components.GovukRadios
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import uk.gov.hmrc.hmrcfrontend.views.config.{HmrcPageHeadingLegend, HmrcSectionCaption}
import viewmodels.LegendSize.Large
import viewmodels.govuk.all.{FluentLegend, RadiosViewModel}
import viewmodels.implicits.*

import javax.inject.Inject

class ChooseAddressViewModel @Inject() (
  govukRadios: GovukRadios
) {
  def apply(
    form: Form[_],
    addresses: Seq[LookupAddress]
  )(implicit messages: Messages): Html =
    HtmlFormat.fill(
      Seq(
        govukRadios(
          RadiosViewModel(
            field = form("value"),
            legend = HmrcPageHeadingLegend
              .apply(
                Text(messages("chooseAddress.heading")),
                HmrcSectionCaption(messages("sectionTitle.orgDetails")),
                "govuk-fieldset__legend--l"
              )
              .asPageHeading(Large),
            items = addressItems(addresses)
          )
        )
      )
    )

  private def addressItems(
    addresses: Seq[LookupAddress]
  )(implicit messages: Messages): Seq[RadioItem] =
    addresses.zipWithIndex.map { case (address, index) =>
      RadioItem(
        content = HtmlContent(formatAddress(address)),
        value = Some(index.toString)
      )
    } ++ Seq(
      RadioItem(
        divider = Some(messages("chooseAddress.radio.divider"))
      ),
      RadioItem(
        content = Text(messages("chooseAddress.none.of.these")),
        value = Some("none")
      )
    )

  private def formatAddress(address: LookupAddress): String =
    Seq(
      address.addressLine1,
      address.addressLine2,
      address.addressLine3,
      address.postCode
    ).flatten.mkString(", ")
}
