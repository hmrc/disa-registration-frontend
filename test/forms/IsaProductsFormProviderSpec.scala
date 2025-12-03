package forms

import forms.behaviours.CheckboxFieldBehaviours
import models.IsaProducts
import play.api.data.FormError

class IsaProductsFormProviderSpec extends CheckboxFieldBehaviours {

  val form = new IsaProductsFormProvider()()

  ".value" - {

    val fieldName   = "value"
    val requiredKey = "isaProducts.error.required"

    behave like checkboxField[IsaProducts](
      form,
      fieldName,
      validValues = IsaProducts.values,
      invalidError = FormError(s"$fieldName[0]", "error.invalid")
    )

    behave like mandatoryCheckboxField(
      form,
      fieldName,
      requiredKey
    )
  }
}
