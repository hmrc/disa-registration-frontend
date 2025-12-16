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

import base.SpecBase
import forms.behaviours.StringFieldBehaviours
import play.api.data.{Form, FormError}
import play.api.test.Helpers

class PeerToPeerPlatformNumberFormProviderSpec extends SpecBase with StringFieldBehaviours {

  private val requiredKey = "peerToPeerPlatformNumber.error.required"
  private val patternKey  = "peerToPeerPlatformNumber.error.pattern"
  private val pattern     = """^[0-9]{6,7}$""".r

  val form: Form[String] = new PeerToPeerPlatformNumberFormProvider().apply(testString)(Helpers.stubMessages())

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      numericOfLength(6, 7)
    )

    behave like fieldWithPattern(
      form,
      fieldName,
      pattern = pattern,
      error = FormError(fieldName, patternKey, Seq(pattern.toString))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
