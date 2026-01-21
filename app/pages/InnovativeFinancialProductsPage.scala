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

import models.journeydata.isaproducts.{InnovativeFinancialProduct, IsaProducts}

case object InnovativeFinancialProductsPage extends PageWithDependents[IsaProducts] {

  override def toString: String = "innovativeFinancialProducts"

  override def clearAnswer(sectionAnswers: IsaProducts): IsaProducts = sectionAnswers.copy(innovativeFinancialProducts = None)

  override def pagesToClear(before: IsaProducts, after: IsaProducts): List[Page[IsaProducts]] = {
    val dependenciesNeedClearing = hasP2pWith36H(before) && !hasP2pWith36H(after)

    if (dependenciesNeedClearing)
      List(PeerToPeerPlatformPage, PeerToPeerPlatformNumberPage)
    else Nil
  }

  def resumeNormalMode(before: IsaProducts, after: IsaProducts): Boolean =
    !hasP2pWith36H(before) && hasP2pWith36H(after)

  private def hasP2pWith36H(sectionAnswers: IsaProducts): Boolean =
    sectionAnswers.innovativeFinancialProducts.exists(_.contains(InnovativeFinancialProduct.PeertopeerLoansUsingAPlatformWith36hPermissions))
}
