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
import models.journeydata.orgdetails.AddAnotherAddressForm
import play.api.data.Form
import play.api.data.Forms.{mapping, optional}

import javax.inject.Inject

class AddAnotherAddressFormProvider @Inject() extends Mappings {

  def apply(): Form[AddAnotherAddressForm] =
    Form(
      mapping(
        "postcode" -> text("AddAnotherAddress.postcode.error.required")
          .transform(
            AddAnotherAddressFormProvider.normalisePostcode,
            identity
          )
          .verifying(
            "AddAnotherAddress.postcode.error.invalid",
            postcode =>
              AddAnotherAddressFormProvider
                .isValidPostcode(postcode)
          ),

        "filter" -> optional(
          text()
            .transform(_.trim, identity)
            .verifying(
              maxLength(
                255,
                "AddAnotherAddress.filter.error.length"
              )
            )
        )
      )(
        (postcode, filter) =>
          AddAnotherAddressForm(postcode, filter)
      )(
        form =>
          Some(
            (form.postcode, form.filter)
          )
      )
    )
}

object AddAnotherAddressFormProvider {

  import java.util.regex.Pattern

  // Mirrors address-lookup-frontend approach

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
    postcode.trim
      .replaceAll("[ \\t]+", "")
      .toUpperCase
}