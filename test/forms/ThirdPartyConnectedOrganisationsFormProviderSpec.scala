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

import forms.behaviours.StringFieldBehaviours
import models.journeydata.thirdparty.ConnectedThirdPartySelection.noneAreConnectedFormValue
import play.api.data.FormError

class ThirdPartyConnectedOrganisationsFormProviderSpec extends StringFieldBehaviours {

  val requiredKey                    = "thirdPartyConnectedOrganisations.error.required"
  val singleOrganisationKey          = "thirdPartyConnectedOrganisations.error.singleOrganisation"
  val noneAndOrganisationSelectedKey = "thirdPartyConnectedOrganisations.error.noneAndOrganisationSelected"

  val form = new ThirdPartyConnectedOrganisationsFormProvider()()

  ".value" - {

    val fieldName = "value"

    "must bind valid organisation ids" in {

      val validValues = Seq("1", "abc", "org-123")

      validValues.foreach { value =>
        val result =
          form.bind(
            Map(
              "value[0]" -> value
            )
          )

        result.errors must contain(
          FormError(fieldName, singleOrganisationKey)
        )
      }
    }

    "must bind multiple selected values" in {

      val result =
        form.bind(
          Map(
            "value[0]" -> "1",
            "value[1]" -> "2"
          )
        )

      result.errors mustBe empty
      result.value.value mustEqual Seq("1", "2")
    }

    "must bind when only none selected" in {

      val result =
        form.bind(
          Map(
            "value[0]" -> noneAreConnectedFormValue
          )
        )

      result.errors mustBe empty
      result.value.value mustEqual Seq(noneAreConnectedFormValue)
    }

    "must fail when no values are provided" in {

      val result = form.bind(Map.empty[String, String])

      result.errors must contain(
        FormError(fieldName, requiredKey)
      )
    }

    "must fail when values are empty strings" in {

      val result =
        form.bind(
          Map(
            "value[]" -> "",
            "value[]" -> ""
          )
        )

      result.errors must contain(
        FormError(fieldName, requiredKey)
      )
    }

    "must fail when only empty values are submitted" in {

      val result =
        form.bind(
          Map(
            "value[]" -> ""
          )
        )

      result.errors must contain(
        FormError(fieldName, requiredKey)
      )
    }

    "must fail when only one organisation is selected" in {

      val result =
        form.bind(
          Map(
            "value[0]" -> "1"
          )
        )

      result.errors must contain(
        FormError(fieldName, singleOrganisationKey)
      )
    }

    "must fail when none selected and organisation selected together" in {

      val result =
        form.bind(
          Map(
            "value[0]" -> "1",
            "value[1]" -> noneAreConnectedFormValue
          )
        )

      result.errors must contain(
        FormError(fieldName, noneAndOrganisationSelectedKey)
      )
    }

    "must bind mixed valid values correctly" in {

      val result =
        form.bind(
          Map(
            "value[1]" -> "1",
            "value[2]" -> "2",
            "value[3]" -> "3"
          )
        )

      result.errors mustBe empty
      result.value.value mustEqual Seq("1", "2", "3")
    }
  }
}
