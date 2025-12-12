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

package services

import base.SpecBase
import connectors.DisaRegistrationConnector
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock

import scala.concurrent.Future

class JourneyAnswersServiceSpec extends SpecBase {

  val mockConnector: DisaRegistrationConnector =
    mock[DisaRegistrationConnector]

  val service = new JourneyAnswersService(mockConnector)

  "JourneyAnswersService" - {

    "get" - {

      "must return Some(JourneyData)) when connector returns valid JSON in a HttpResponse" in {
        when(mockConnector.getJourneyData(ArgumentMatchers.eq(testGroupId))(any()))
          .thenReturn(Future.successful(Some(testJourneyData)))

        val result = service.get(testGroupId).futureValue

        result mustBe Some(testJourneyData)
      }

      "must return None when connector returns a None" in {
        when(mockConnector.getJourneyData(ArgumentMatchers.eq(testGroupId))(any()))
          .thenReturn(Future.successful(None))

        val result = service.get(testGroupId).futureValue

        result mustBe None
      }

      "must propagate exception from connector" in {
        val ex = new Exception

        when(mockConnector.getJourneyData(ArgumentMatchers.eq(testGroupId))(any()))
          .thenReturn(Future.failed(ex))

        val thrown = service.get(testGroupId).failed.futureValue

        thrown mustBe ex
      }
    }

    "update" - {

      "must complete successfully when connector returns Unit" in {
        when(
          mockConnector.updateTaskListJourney(
            ArgumentMatchers.eq(testIsaProductsAnswers),
            ArgumentMatchers.eq(testGroupId),
            ArgumentMatchers.eq(testIsaProductsAnswers.sectionName)
          )(any(), any())
        ).thenReturn(Future.successful(()))

        val result: Unit = service.update(testIsaProductsAnswers, testGroupId).futureValue

        result mustBe ()
        verify(mockConnector)
          .updateTaskListJourney(testIsaProductsAnswers, testGroupId, testIsaProductsAnswers.sectionName)
      }

      "must propagate exception from connector" in {
        val ex = new Exception

        when(
          mockConnector.updateTaskListJourney(
            ArgumentMatchers.eq(testIsaProductsAnswers),
            ArgumentMatchers.eq(testGroupId),
            ArgumentMatchers.eq(testIsaProductsAnswers.sectionName)
          )(any(), any())
        ).thenReturn(Future.failed(ex))

        val thrown = service.update(testIsaProductsAnswers, testGroupId).failed.futureValue
        thrown mustBe ex
      }
    }
  }
}
