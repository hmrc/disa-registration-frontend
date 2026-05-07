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

import play.api.data.Form
import play.api.data.Forms.{nonEmptyText, seq}

import javax.inject.Inject

class ThirdPartyConnectedOrganisationsFormProvider @Inject {

  // TODO: Waiting on confirmation from UCD but we require two error message
  // 1. if only selected one org
  // 2. if JS is disabled and user selects none&orgs
  def apply(): Form[Seq[String]] =
    Form(
      "value" ->
        seq(nonEmptyText)
          .verifying(
            "thirdPartyConnectedOrganisations.error.required",
            _.nonEmpty
          )
    )
}
