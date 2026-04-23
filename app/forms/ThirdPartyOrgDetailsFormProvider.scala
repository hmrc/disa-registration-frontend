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
import models.journeydata.thirdparties.ThirdPartyOrgDetailsForm
import play.api.data.Form
import play.api.data.Forms.{mapping, optional}

import javax.inject.Inject

class ThirdPartyOrgDetailsFormProvider @Inject() extends Mappings {

  def apply(): Form[ThirdPartyOrgDetailsForm] =
    Form(
      mapping(
        "thirdPartyName" -> text("thirdPartyOrgDetails.thirdPartyName.error.required")
          .transform(_.trim, identity)
          .verifying(regexp(ThirdPartyOrgDetailsFormProvider.thirdPartyNameRegex, "thirdPartyOrgDetails.thirdPartyName.error.invalid")),

        "frn" -> optional(
          text()
            .transform(_.trim, identity)
            .verifying(minLength(6, "thirdPartyOrgDetails.frn.error.incorrectLength"))
            .verifying(maxLength(7, "thirdPartyOrgDetails.frn.error.incorrectLength"))
            .verifying(regexp(ThirdPartyOrgDetailsFormProvider.frnRegex, "thirdPartyOrgDetails.frn.error.invalid"))
        )
      )(
        (name, frn) => ThirdPartyOrgDetailsForm(name, frn)
      )(
        form => Some((form.name, form.frn))
      )
    )
}

object ThirdPartyOrgDetailsFormProvider {
  private[forms] val thirdPartyNameRegex = "^[\\p{L}'’ -]+$"
  private[forms] val frnRegex = "^[0-9]+$"
}
