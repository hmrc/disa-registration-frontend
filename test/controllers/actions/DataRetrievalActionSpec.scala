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

package controllers.actions

import base.SpecBase
import models.journeyData.JourneyData
import models.requests.{IdentifierRequest, OptionalDataRequest}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import services.JourneyAnswersService

import scala.concurrent.Future

class DataRetrievalActionSpec extends SpecBase with MockitoSugar {

  class Harness(journeyAnswersService: JourneyAnswersService) extends DataRetrievalActionImpl(journeyAnswersService) {
    def callRefine[A](request: IdentifierRequest[A]): Future[Either[Result, OptionalDataRequest[A]]] = refine(request)
  }

  "Data Retrieval Action" - {

    "when NOT FOUND is returned" - {

      "must set userAnswers to 'None' in the request" in {
        when(mockJourneyAnswersService.get(ArgumentMatchers.eq("id"))(any)) thenReturn Future.successful(None)
        val action = new Harness(mockJourneyAnswersService)

        val Right(result) = action.callRefine(IdentifierRequest(FakeRequest(), "id")).futureValue

        result.journeyData must not be defined
      }
    }

    "when there is data in the cache" - {

      "must build a userAnswers object and add it to the request" in {
        when(mockJourneyAnswersService.get(ArgumentMatchers.eq("id"))(any)) thenReturn Future.successful(
          Some(JourneyData("id"))
        )
        val action = new Harness(mockJourneyAnswersService)

        val Right(result) = action.callRefine(IdentifierRequest(FakeRequest(), "id")).futureValue

        result.journeyData mustBe defined
      }
    }

    "when an upstream error response occurs" - {

      "must give an InternalServerError result" in {
        val ex     = new Exception
        when(mockJourneyAnswersService.get(ArgumentMatchers.eq("id"))(any)) thenReturn Future.failed(ex)
        val action = new Harness(mockJourneyAnswersService)

        val Left(result) = action.callRefine(IdentifierRequest(FakeRequest(), "id")).futureValue

        status(Future(result)) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
