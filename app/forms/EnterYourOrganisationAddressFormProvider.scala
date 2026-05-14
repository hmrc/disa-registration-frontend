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

import javax.inject.Inject
import forms.mappings.Mappings
import play.api.data.Form
import play.api.data.Forms.*
import models.journeydata.CorrespondenceAddress

class EnterYourOrganisationAddressFormProvider @Inject() extends Mappings {

  def apply(): Form[CorrespondenceAddress] = Form(
    mapping(
      "addressLine1" -> text("enterYourOrganisationAddress.error.addressLine1.required")
        .transform(_.trim, identity)
        .verifying(maxLength(255, "enterYourOrganisationAddress.error.addressLine1.length")),
      "addressLine2" -> optional(
        text()
          .transform(_.trim, identity)
          .verifying(maxLength(255, "enterYourOrganisationAddress.error.addressLine2.length"))
      ),
      "townOrCity"   -> text("enterYourOrganisationAddress.error.townOrCity.required")
        .transform(_.trim, identity)
        .verifying(maxLength(255, "enterYourOrganisationAddress.error.townOrCity.length")),
      "postcode"     -> text("enterYourOrganisationAddress.error.postcode.required")
        .transform(_.trim, identity)
        .verifying(minLength(5, "enterYourOrganisationAddress.error.postcode.tooShort"))
        .verifying(maxLength(8, "enterYourOrganisationAddress.error.postcode.tooLong"))
        .verifying(
          "enterYourOrganisationAddress.error.postcode.invalid",
          AddAnotherAddressFormProvider.isValidPostcode
        )
    ) { case (addressLine1, addressLine2, townOrCity, postcode) =>
      CorrespondenceAddress(
        addressLine1 = Some(addressLine1),
        addressLine2 = addressLine2,
        addressLine3 = Some(townOrCity),
        postCode = Some(postcode)
      )
    } { address =>
      Some(
        (
          address.addressLine1.getOrElse(""),
          address.addressLine2,
          address.addressLine3.getOrElse(""),
          address.postCode.getOrElse("")
        )
      )
    }
  )
}
