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

import forms.behaviours.CheckboxFieldBehaviours
import models.FcaArticles
import play.api.data.FormError

class FcaArticlesFormProviderSpec extends CheckboxFieldBehaviours {

  val form = new FcaArticlesFormProvider()()

  ".value" - {

    val fieldName   = "value"
    val requiredKey = "fcaArticles.error.required"

    behave like checkboxField[FcaArticles](
      form,
      fieldName,
      validValues = FcaArticles.values,
      invalidError = FormError(s"$fieldName[0]", "error.invalid")
    )

    behave like mandatoryCheckboxField(
      form,
      fieldName,
      requiredKey
    )
  }
}
