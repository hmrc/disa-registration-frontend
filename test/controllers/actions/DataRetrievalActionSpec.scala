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
import play.api.test.FakeRequest
import services.JourneyAnswersService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataRetrievalActionSpec extends SpecBase with MockitoSugar {

  class Harness(journeyAnswersService: JourneyAnswersService) extends DataRetrievalActionImpl(journeyAnswersService) {
    def callTransform[A](request: IdentifierRequest[A]): Future[OptionalDataRequest[A]] = transform(request)
  }

  "Data Retrieval Action" - {

    "when there is no data in the cache" - {

      "must set userAnswers to 'None' in the request" in {
        when(mockJourneyAnswersService.get(ArgumentMatchers.eq("id"))(any)) thenReturn Future(None)
        val action = new Harness(mockJourneyAnswersService)

        val result = action.callTransform(IdentifierRequest(FakeRequest(), "id")).futureValue

        result.journeyData must not be defined
      }
    }

    "when there is data in the cache" - {

      "must build a userAnswers object and add it to the request" in {
        when(mockJourneyAnswersService.get(ArgumentMatchers.eq("id"))(any)) thenReturn Future(Some(JourneyData("id")))
        val action = new Harness(mockJourneyAnswersService)

        val result = action.callTransform(IdentifierRequest(FakeRequest(), "id")).futureValue

        result.journeyData mustBe defined
      }
    }
  }
}
