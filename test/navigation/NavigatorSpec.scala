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

import base.SpecBase
import controllers.isaproducts.routes._
import controllers.routes.IndexController
import pages.*
import models.*
import models.journeydata.isaproducts.InnovativeFinancialProduct.{CrowdFundedDebentures, PeertopeerLoansUsingAPlatformWith36hPermissions}
import models.journeydata.isaproducts.IsaProduct.InnovativeFinanceIsas
import models.journeydata.isaproducts.{InnovativeFinancialProduct, IsaProduct, IsaProducts}

class NavigatorSpec extends SpecBase {

  private val navigator = new Navigator()

  private def answersWithIsaProducts(products: Seq[IsaProduct]): IsaProducts =
    IsaProducts(
      isaProducts = Some(products),
      innovativeFinancialProducts = None
    )

  private def answersWithInnovativeFinancialProducts(ifps: Seq[InnovativeFinancialProduct]): IsaProducts =
    IsaProducts(
      isaProducts = Some(Seq(InnovativeFinanceIsas)),
      innovativeFinancialProducts = Some(ifps)
    )

  private val emptyAnswers: IsaProducts =
    IsaProducts(
      isaProducts = None,
      innovativeFinancialProducts = None
    )

  "Navigator.nextPage" - {

    "must go from a page that doesn't exist in the route map to Index" in {
      case object UnknownPage extends Page[IsaProducts]

      navigator.nextPage(UnknownPage, emptyAnswers, NormalMode) mustBe
        IndexController.onPageLoad()
    }

    "when on IsaProductsPage" - {

      "must redirect to Index when there is no ISA products data" in {
        navigator.nextPage(IsaProductsPage, emptyAnswers, NormalMode) mustBe
          IndexController.onPageLoad()
      }

      "must go to InnovativeFinancialProducts when Innovative Finance ISAs is selected (NormalMode)" in {
        val answers = answersWithIsaProducts(Seq(InnovativeFinanceIsas))

        navigator.nextPage(IsaProductsPage, answers, NormalMode) mustBe
          InnovativeFinancialProductsController.onPageLoad(NormalMode)
      }

      "must go to Check Your Answers when Innovative Finance ISAs is NOT selected (NormalMode)" in {
        val answers = answersWithIsaProducts(Seq(IsaProduct.CashIsas))

        navigator.nextPage(IsaProductsPage, answers, NormalMode) mustBe
          IsaProductsCheckYourAnswersController.onPageLoad()
      }

      "must respect mode when routing to InnovativeFinancialProducts (CheckMode)" in {
        val answers = answersWithIsaProducts(Seq(InnovativeFinanceIsas))

        navigator.nextPage(IsaProductsPage, answers, CheckMode) mustBe
          InnovativeFinancialProductsController.onPageLoad(CheckMode)
      }
    }

    "when on InnovativeFinancialProductsPage" - {

      "must redirect to Index when there is no innovative financial products data" in {
        navigator.nextPage(InnovativeFinancialProductsPage, emptyAnswers, NormalMode) mustBe
          IndexController.onPageLoad()
      }

      "must go to PeerToPeerPlatform when 36H peer-to-peer option is selected (NormalMode)" in {
        val answers = answersWithInnovativeFinancialProducts(
          Seq(PeertopeerLoansUsingAPlatformWith36hPermissions)
        )

        navigator.nextPage(InnovativeFinancialProductsPage, answers, NormalMode) mustBe
          PeerToPeerPlatformController.onPageLoad(NormalMode)
      }

      "must go to Check Your Answers when 36H peer-to-peer option is NOT selected (NormalMode)" in {
        val answers = answersWithInnovativeFinancialProducts(
          Seq(CrowdFundedDebentures)
        )

        navigator.nextPage(InnovativeFinancialProductsPage, answers, NormalMode) mustBe
          IsaProductsCheckYourAnswersController.onPageLoad()
      }

      "must respect mode when routing to PeerToPeerPlatform (CheckMode)" in {
        val answers = answersWithInnovativeFinancialProducts(
          Seq(PeertopeerLoansUsingAPlatformWith36hPermissions)
        )

        navigator.nextPage(InnovativeFinancialProductsPage, answers, CheckMode) mustBe
          PeerToPeerPlatformController.onPageLoad(CheckMode)
      }
    }

    "when on PeerToPeerPlatformPage" - {

      "must go to PeerToPeerPlatformNumberController (NormalMode)" in {
        navigator.nextPage(PeerToPeerPlatformPage, emptyAnswers, NormalMode) mustBe
          PeerToPeerPlatformNumberController.onPageLoad(NormalMode)
      }

      "must go to PeerToPeerPlatformNumberController (CheckMode)" in {
        navigator.nextPage(PeerToPeerPlatformPage, emptyAnswers, CheckMode) mustBe
          PeerToPeerPlatformNumberController.onPageLoad(CheckMode)
      }
    }

    "when on PeerToPeerPlatformNumberPage" - {

      "must go to Check Your Answers (NormalMode)" in {
        navigator.nextPage(PeerToPeerPlatformNumberPage, emptyAnswers, NormalMode) mustBe
          IsaProductsCheckYourAnswersController.onPageLoad()
      }

      "must go to Check Your Answers (CheckMode)" in {
        navigator.nextPage(PeerToPeerPlatformNumberPage, emptyAnswers, CheckMode) mustBe
          IsaProductsCheckYourAnswersController.onPageLoad()
      }
    }
  }
}
