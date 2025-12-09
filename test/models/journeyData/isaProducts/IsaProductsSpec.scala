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

package models.journeyData.isaProducts

import models.journeyData.isaProducts.IsaProduct.CashIsas
import play.api.libs.json.{JsValue, Json, OFormat}
import utils.JsonFormatSpec

class IsaProductsSpec extends JsonFormatSpec[IsaProducts] {

  override val model =
    IsaProducts(isaProducts = Some(Seq(CashIsas)), dataItem2 = None)

  override val json: JsValue = Json.parse("""
    {
     "isaProducts": ["cashIsas"]
    }
  """)

  override implicit val format: OFormat[IsaProducts] = IsaProducts.format
}
