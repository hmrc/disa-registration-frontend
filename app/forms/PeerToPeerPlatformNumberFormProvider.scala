/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.i18n.Messages

class PeerToPeerPlatformNumberFormProvider @Inject() extends Mappings {

  private val validPattern = """^[0-9]{6,7}$"""

  def apply(platformName: String)(implicit messages: Messages): Form[String] =
    Form(
      "value" -> text(messages("peerToPeerPlatformNumber.error.required", platformName))
        .verifying(regexp(validPattern, messages("peerToPeerPlatformNumber.error.pattern", platformName)))
    )
}
