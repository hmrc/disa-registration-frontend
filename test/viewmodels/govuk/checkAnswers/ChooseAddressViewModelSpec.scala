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

package viewmodels.govuk.checkAnswers

import base.SpecBase
import models.addresslookup.LookupAddress
import org.jsoup.Jsoup
import play.api.data.Form
import play.api.test.Helpers.*
import viewmodels.address.ChooseAddressViewModel

class ChooseAddressViewModelSpec extends SpecBase {

  private val form: Form[_] = Form("value" -> play.api.data.Forms.text())

  private val address1 =
    LookupAddress(
      addressLine1 = Some("1 Test Street"),
      addressLine2 = Some("London"),
      addressLine3 = None,
      postCode = Some("SW1A 1AA")
    )

  private val address2 =
    LookupAddress(
      addressLine1 = Some("2 Example Road"),
      addressLine2 = Some("London"),
      addressLine3 = None,
      postCode = Some("SW1A 2BB")
    )

  "ChooseAddressViewModel" - {

    "must render all addresses as radio items" in {

      running(app) {
        val viewModel = app.injector.instanceOf[ChooseAddressViewModel]

        val html = viewModel(
          form,
          Seq(address1, address2)
        )(messages(app))

        val doc = Jsoup.parse(html.toString)

        doc.text() must include("1 Test Street")
        doc.text() must include("2 Example Road")

        val radios = doc.select("input[type=radio][name=value]")
        radios.size() mustEqual 3
      }
    }

    "must render a divider and 'none of these' option" in {

      running(app) {
        val viewModel = app.injector.instanceOf[ChooseAddressViewModel]

        val html = viewModel(
          form,
          Seq(address1)
        )(messages(app))

        val doc = Jsoup.parse(html.toString)

        doc.text() must include(messages(app)("chooseAddress.none.of.these"))
        doc.text() must include(messages(app)("chooseAddress.radio.divider"))
      }
    }

    "must render correct number of radio inputs" in {

      running(app) {
        val viewModel = app.injector.instanceOf[ChooseAddressViewModel]

        val html = viewModel(
          form,
          Seq(address1, address2)
        )(messages(app))

        val doc = Jsoup.parse(html.toString)

        doc.select("input[type=radio][name=value]").size() mustEqual 3
      }
    }

    "must format addresses correctly" in {

      running(app) {
        val viewModel = app.injector.instanceOf[ChooseAddressViewModel]

        val html = viewModel(
          form,
          Seq(address1)
        )(messages(app))

        val doc = Jsoup.parse(html.toString)

        doc.text() must include("1 Test Street, London, SW1A 1AA")
      }
    }

    "must not break when optional address lines are missing" in {

      running(app) {
        val viewModel = app.injector.instanceOf[ChooseAddressViewModel]

        val minimalAddress =
          LookupAddress(
            addressLine1 = Some("Single Line"),
            addressLine2 = None,
            addressLine3 = None,
            postCode = Some("AB1 2CD")
          )

        val html = viewModel(
          form,
          Seq(minimalAddress)
        )(messages(app))

        val doc = Jsoup.parse(html.toString)

        doc.text() must include("Single Line")
        doc.text() must include("AB1 2CD")
      }
    }

    "must render radios in correct order (addresses first, then none option)" in {

      running(app) {
        val viewModel = app.injector.instanceOf[ChooseAddressViewModel]

        val html = viewModel(
          form,
          Seq(address1)
        )(messages(app))

        val doc  = Jsoup.parse(html.toString)
        val text = doc.text()

        val addressIndex = text.indexOf("1 Test Street")
        val noneIndex    = text.indexOf(messages(app)("chooseAddress.none.of.these"))

        addressIndex must be < noneIndex
      }
    }
  }
}
