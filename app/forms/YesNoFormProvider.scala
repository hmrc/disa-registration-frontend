package forms

import forms.mappings.Mappings
import play.api.data.Form

class YesNoFormProvider extends Mappings {
  
  def apply(errorKey: String): Form[Boolean] =
    Form(
      "value" -> boolean(errorKey)
    )
}
