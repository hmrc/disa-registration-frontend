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

package forms

import forms.mappings.Mappings
import models.journeydata.orgdetails.AddAnotherAddress
import play.api.data.Form
import play.api.data.Forms.{mapping, optional}

import javax.inject.Inject

class AddAnotherAddressFormProvider @Inject() extends Mappings {

  def apply(): Form[AddAnotherAddress] =
    Form(
      mapping(
        "postcode" ->
          text("AddAnotherAddress.postcode.error.required")
            .transform(_.trim, identity)
            .verifying(
              "AddAnotherAddress.postcode.error.tooShort",
              _.length >= 5
            )
            .verifying(
              "AddAnotherAddress.postcode.error.tooLong",
              _.length <= 8
            )
            .verifying(
              "AddAnotherAddress.postcode.error.invalid",
              AddAnotherAddressFormProvider.isValidPostcode
            ),
        "filter"   ->
          optional(
            text()
              .transform(_.trim, identity)
              .verifying(
                "AddAnotherAddress.filter.error.incorrectLength",
                _.length <= 255
              )
          )
      )((postcode, filter) =>
        AddAnotherAddress(
          postcode = postcode,
          filter = filter,
          addresses = Seq.empty,
          selectedAddress = None
        )
      )(form =>
        Some(
          (form.postcode, form.filter)
        )
      )
    )
}

object AddAnotherAddressFormProvider {

  import java.util.regex.Pattern

  private val outcodePattern =
    Pattern.compile("^GIR|[A-Z]{1,2}[0-9][0-9A-Z]?$")

  private val incodePattern =
    Pattern.compile("^[0-9][A-Z]{2}$")

  def isValidPostcode(postcode: String): Boolean = {

    val normalised = normalisePostcode(postcode)

    if (normalised.length < 5) {
      false
    } else {

      val splitIndex = normalised.length - 3

      val outcode = normalised.substring(0, splitIndex)
      val incode  = normalised.substring(splitIndex)

      outcodePattern.matcher(outcode).matches() &&
      incodePattern.matcher(incode).matches()
    }
  }

  def normalisePostcode(postcode: String): String =
    postcode
      .replaceAll("\\s+", "")
      .toUpperCase
}
