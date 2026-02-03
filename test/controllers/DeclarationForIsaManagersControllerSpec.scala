/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers

import base.SpecBase
import controllers.routes.IndexController
import models.journeydata.isaproducts.IsaProduct
import models.journeydata.isaproducts.IsaProduct.{CashIsas, CashJuniorIsas, StocksAndShareJuniorIsas, StocksAndSharesIsas}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import play.api.inject.bind
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{JourneyAnswersService, SubmissionService}
import views.html.DeclarationForIsaManagersView

import scala.concurrent.Future

class DeclarationForIsaManagersControllerSpec extends SpecBase {

  "DeclarationForIsaManagers Controller" - {

    "GET" - {

      val cases: Seq[(Seq[IsaProduct], Boolean, String)] = Seq(
        (Seq(CashJuniorIsas), true, "single junior"),
        (Seq(CashIsas), false, "single non-junior"),
        (Seq(CashJuniorIsas, CashIsas), true, "junior + non-junior"),
        (Seq(CashIsas, StocksAndSharesIsas), false, "multiple non-junior"),
        (Seq(CashJuniorIsas, StocksAndShareJuniorIsas), true, "multiple junior")
      )

      cases.foreach { case (isaTypes, expectedHasJunior, label) =>
        s"must return OK and correct view for GET when selected ISAs are $label" in {

          val jd          = emptyJourneyData.withIsaProducts(isaTypes: _*)
          val application = applicationBuilder(journeyData = Some(jd)).build()

          running(application) {
            val request = FakeRequest(GET, routes.DeclarationForIsaManagersController.onPageLoad().url)
            val result  = route(application, request).value

            val view = application.injector.instanceOf[DeclarationForIsaManagersView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(expectedHasJunior)(request, messages(application)).toString
            if (expectedHasJunior) {
              contentAsString(result) must include(messages("declarationForIsaManagers.junior.li1"))
              contentAsString(result) must include(messages("declarationForIsaManagers.junior.li2"))
            }
          }
        }
      }

      "must redirect to tasklist if requisite data is not held in the journey data" in {
        val jd          = emptyJourneyData
        val application = applicationBuilder(journeyData = Some(jd)).build()

        running(application) {
          val request = FakeRequest(GET, routes.DeclarationForIsaManagersController.onPageLoad().url)
          val result  = route(application, request).value

          status(result) mustEqual 303
          redirectLocation(result) mustBe Some(IndexController.onPageLoad().url)
        }
      }
    }

    "POST" - {

      "must redirect to confirmation page when submission succeeds" in {

        val jd          = emptyJourneyData.withIsaProducts(CashIsas)
        val application =
          applicationBuilder(journeyData = Some(jd), bind[SubmissionService].toInstance(mockSubmissionService)).build()

        val receiptId = "receipt-123"

        when(mockSubmissionService.declareAndSubmit(any(), any(), eqTo(jd))(any(), any()))
          .thenReturn(Future.successful(receiptId))

        running(application) {
          val request = FakeRequest(POST, routes.DeclarationForIsaManagersController.onSubmit().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result) mustBe Some(routes.ConfirmationController.onPageLoad(receiptId).url)
        }
      }

      "must return Internal Server Error when submission throws" in {

        val jd          = emptyJourneyData.withIsaProducts(CashIsas)
        val application = applicationBuilder(journeyData = Some(jd)).build()

        when(mockSubmissionService.declareAndSubmit(any(), any(), eqTo(jd))(any(), any()))
          .thenReturn(Future.failed(new Exception("fubar")))

        running(application) {
          val request = FakeRequest(POST, routes.DeclarationForIsaManagersController.onSubmit().url)

          val result = route(application, request).value

          await(result)

          verify(mockErrorHandler).internalServerError(any[RequestHeader])
        }
      }
    }
  }
}
