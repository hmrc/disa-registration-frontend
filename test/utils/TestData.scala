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

import models.journeydata.JourneyData
import models.journeydata.isaproducts.{InnovativeFinancialProduct, IsaProduct, IsaProducts}

trait TestData {
  val testGroupId: String = "id"

  def emptyJourneyData: JourneyData = JourneyData(testGroupId)

  val testIsaProductsAnswers: IsaProducts =
    IsaProducts(
      isaProducts = Some(IsaProduct.values),
      p2pPlatform = Some("Test Platform"),
      p2pPlatformNumber = Some("1234567"),
      innovativeFinancialProducts = Some(InnovativeFinancialProduct.values)
    )
  val testJourneyData: JourneyData        = JourneyData(groupId = testGroupId, isaProducts = Some(testIsaProductsAnswers))
  val testString                          = "test"
  val testZRef                            = "Z1234"
}
