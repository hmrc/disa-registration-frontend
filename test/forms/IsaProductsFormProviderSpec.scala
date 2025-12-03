package forms

import forms.behaviours.CheckboxFieldBehaviours
import models.IsaProduct
import play.api.data.FormError

class IsaProductsFormProviderSpec extends CheckboxFieldBehaviours {

  val form = new IsaProductsFormProvider()()

  ".value" - {

    val fieldName   = "value"
    val requiredKey = "isaProducts.error.required"

    behave like checkboxField[IsaProduct](
      form,
      fieldName,
      validValues = IsaProduct.values,
      invalidError = FormError(s"$fieldName[0]", "error.invalid")
    )

    behave like mandatoryCheckboxField(
      form,
      fieldName,
      requiredKey
    )
  }
}
