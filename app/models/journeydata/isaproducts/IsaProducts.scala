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

package models.journeydata.isaproducts

import models.journeydata.TaskListSection
import models.journeydata.isaproducts.InnovativeFinancialProduct.PeertopeerLoansUsingAPlatformWith36hPermissions
import models.journeydata.isaproducts.IsaProduct.*
import play.api.libs.json.{Json, OFormat}

case class IsaProducts(
  isaProducts: Option[Seq[IsaProduct]] = None,
  p2pPlatform: Option[String] = None,
  p2pPlatformNumber: Option[String] = None,
  innovativeFinancialProducts: Option[Seq[InnovativeFinancialProduct]] = None
) extends TaskListSection {
  override def sectionName: String = IsaProducts.sectionName

  def isComplete: Boolean =
    isaProducts.exists(_.nonEmpty) &&
      (!hasInnovativeFinanceIsas || innovativeProductsComplete)

  private def hasInnovativeFinanceIsas: Boolean =
    isaProducts.exists(_.contains(InnovativeFinanceIsas))

  private def hasP2pProduct: Boolean =
    innovativeFinancialProducts.exists(_.contains(PeertopeerLoansUsingAPlatformWith36hPermissions))

  private def innovativeProductsComplete: Boolean =
    innovativeFinancialProducts.exists(_.nonEmpty) &&
      (!hasP2pProduct || p2pDetailsComplete)

  private def p2pDetailsComplete: Boolean =
    p2pPlatform.exists(_.nonEmpty) && p2pPlatformNumber.exists(_.nonEmpty)
}

object IsaProducts {
  implicit val format: OFormat[IsaProducts] = Json.format[IsaProducts]

  val sectionName = "isaProducts"
}
