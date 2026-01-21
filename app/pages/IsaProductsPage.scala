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

package pages

import models.journeydata.isaproducts.{IsaProduct, IsaProducts}

case object IsaProductsPage extends PageWithDependents[IsaProducts] {
  override def toString: String = "isaProducts"

  override def clearAnswer(sectionAnswers: IsaProducts): IsaProducts = sectionAnswers.copy(isaProducts = None)

  override def pagesToClear(before: IsaProducts, after: IsaProducts): List[Page[IsaProducts]] =
    val dependenciesNeedClearing = hasIfIsa(before) && !hasIfIsa(after)

    if (dependenciesNeedClearing)
      List(
        InnovativeFinancialProductsPage,
        PeerToPeerPlatformPage,
        PeerToPeerPlatformNumberPage
      )
    else Nil

  def resumeNormalMode(before: IsaProducts, after: IsaProducts): Boolean =
    !hasIfIsa(before) && hasIfIsa(after)

  private def hasIfIsa(sectionAnswers: IsaProducts): Boolean =
    sectionAnswers.isaProducts.exists(_.contains(IsaProduct.InnovativeFinanceIsas))
}
