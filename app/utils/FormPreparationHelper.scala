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

package utils

import models.journeyData.JourneyData
import models.requests.OptionalDataRequest
import play.api.data.Form

object FormPreparationHelper {

  def prepareForm[T, A](
    form: Form[A]
  )(extractor: JourneyData => Option[T])(toFillValue: T => A)(implicit request: OptionalDataRequest[_]): Form[A] =
    request.journeyData match {
      case Some(journey) =>
        extractor(journey) match {
          case Some(value) => form.fill(toFillValue(value))
          case None        => form
        }
      case None          =>
        form
    }
}
