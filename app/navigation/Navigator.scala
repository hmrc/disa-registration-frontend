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

package navigation

import javax.inject.{Inject, Singleton}
import play.api.mvc.Call
import controllers.routes
import controllers.isaproducts.routes.*
import pages.*
import models.*
import models.journeydata.TaskListSection
import models.journeydata.isaproducts.InnovativeFinancialProduct.PeertopeerLoansUsingAPlatformWith36hPermissions
import models.journeydata.isaproducts.IsaProduct.InnovativeFinanceIsas
import models.journeydata.isaproducts.IsaProducts

@Singleton
class Navigator @Inject() () {

  def nextPage[A <: TaskListSection](page: Page[A], answers: A, mode: Mode): Call = page match {
    case RegisteredIsaManagerPage        => ???
    case ZReferenceNumberPage            => ???
    case IsaProductsPage                 => isaProductsNextPage(answers, mode)
    case InnovativeFinancialProductsPage => innovativeFinancialProductsNextPage(answers, mode)
    case PeerToPeerPlatformPage          => PeerToPeerPlatformNumberController.onPageLoad(mode)
    case PeerToPeerPlatformNumberPage    => IsaProductsCheckYourAnswersController.onPageLoad()
    case _                               => routes.IndexController.onPageLoad()
  }

  private def isaProductsNextPage(answers: IsaProducts, mode: Mode): Call = {
    mode match {
      case NormalMode =>
        answers.isaProducts.fold(routes.IndexController.onPageLoad()) { isaProducts =>
        if (isaProducts.contains(InnovativeFinanceIsas)) InnovativeFinancialProductsController.onPageLoad(mode)
        else IsaProductsCheckYourAnswersController.onPageLoad()
      }
      case CheckMode =>
        answers.isaProducts.fold(routes.IndexController.onPageLoad()) { isaProducts =>
          if (isaProducts.contains(InnovativeFinanceIsas)) InnovativeFinancialProductsController.onPageLoad(mode)
          else IsaProductsCheckYourAnswersController.onPageLoad()
        }
    }

  }

  private def innovativeFinancialProductsNextPage(answers: IsaProducts, mode: Mode): Call =
    answers.innovativeFinancialProducts.fold(routes.IndexController.onPageLoad()) { ifps =>
      if (ifps.contains(PeertopeerLoansUsingAPlatformWith36hPermissions)) PeerToPeerPlatformController.onPageLoad(mode)
      else IsaProductsCheckYourAnswersController.onPageLoad()
    }
}
