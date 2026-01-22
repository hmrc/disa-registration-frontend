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

  def nextPage[A <: TaskListSection](page: Page[A], answers: A, mode: Mode): Call = mode match {
    case NormalMode =>
      normalRoutes(page, answers)
    case CheckMode  =>
      checkRouteMap(page, answers)
  }
  private def normalRoutes[A <: TaskListSection](page: Page[A], answers: A): Call = page match {
    case RegisteredIsaManagerPage        => ???
    case ZReferenceNumberPage            => ???
    case IsaProductsPage                 => isaProductsNextPage(answers)
    case InnovativeFinancialProductsPage => innovativeFinancialProductsNextPage(answers)
    case PeerToPeerPlatformPage          => PeerToPeerPlatformNumberController.onPageLoad(NormalMode)
    case PeerToPeerPlatformNumberPage    => IsaProductsCheckYourAnswersController.onPageLoad()
    case _                               => routes.IndexController.onPageLoad()
  }

  private def checkRouteMap[A <: TaskListSection](page: Page[A], answers: A): Call = page match {
    case RegisteredIsaManagerPage        => ???
    case ZReferenceNumberPage            => ???
    case IsaProductsPage                 => IsaProductsCheckYourAnswersController.onPageLoad()
    case InnovativeFinancialProductsPage => IsaProductsCheckYourAnswersController.onPageLoad()
    case PeerToPeerPlatformPage          => IsaProductsCheckYourAnswersController.onPageLoad()
    case PeerToPeerPlatformNumberPage    => IsaProductsCheckYourAnswersController.onPageLoad()
    case _                               => routes.IndexController.onPageLoad()
  }

  def determineMode[A <: TaskListSection](
                                           currentMode: Mode,
                                           changedPage: PageWithDependents[A],
                                           existing: Option[A],
                                           updated: A
                                         ): Mode =
    existing.fold(NormalMode) { existing =>
      if (changedPage.resumeNormalMode(existing, updated)) NormalMode
      else currentMode
    }

  private def isaProductsNextPage(answers: IsaProducts): Call =
    answers.isaProducts.fold(routes.IndexController.onPageLoad()) { isaProducts =>
      if (isaProducts.contains(InnovativeFinanceIsas)) InnovativeFinancialProductsController.onPageLoad(NormalMode)
      else IsaProductsCheckYourAnswersController.onPageLoad()
    }

  private def innovativeFinancialProductsNextPage(answers: IsaProducts): Call =
    answers.innovativeFinancialProducts.fold(routes.IndexController.onPageLoad()) { ifps =>
      if (ifps.contains(PeertopeerLoansUsingAPlatformWith36hPermissions))
        PeerToPeerPlatformController.onPageLoad(NormalMode)
      else IsaProductsCheckYourAnswersController.onPageLoad()
    }
}
