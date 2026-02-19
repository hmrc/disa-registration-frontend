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
import models.GetOrCreateJourneyData
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}

import scala.concurrent.Future

class JourneyAnswersServiceSpec extends SpecBase {

  val service = new JourneyAnswersService(mockDisaRegistrationConnector, mockSessionRepository)

  "JourneyAnswersService" - {

    "get" - {

      "must return Some(JourneyData)) when connector returns valid JSON in a HttpResponse" in {
        when(mockDisaRegistrationConnector.getJourneyData(ArgumentMatchers.eq(testGroupId))(any()))
          .thenReturn(Future.successful(Some(testJourneyData)))

        val result = service.get(testGroupId).futureValue

        result mustBe Some(testJourneyData)
      }

      "must return None when connector returns a None" in {
        when(mockDisaRegistrationConnector.getJourneyData(ArgumentMatchers.eq(testGroupId))(any()))
          .thenReturn(Future.successful(None))

        val result = service.get(testGroupId).futureValue

        result mustBe None
      }

      "must propagate exception from connector" in {
        val ex = new Exception

        when(mockDisaRegistrationConnector.getJourneyData(ArgumentMatchers.eq(testGroupId))(any()))
          .thenReturn(Future.failed(ex))

        val thrown = service.get(testGroupId).failed.futureValue

        thrown mustBe ex
      }
    }

    "update" - {

      "must complete successfully when connector returns Unit and mark updates in session" in {
        when(mockSessionRepository.upsertAndMarkUpdatesInSession(any[String])).thenReturn(Future.unit)

        when(
          mockDisaRegistrationConnector.updateTaskListJourney(
            ArgumentMatchers.eq(testIsaProductsAnswers),
            ArgumentMatchers.eq(testGroupId),
            ArgumentMatchers.eq(testIsaProductsAnswers.sectionName)
          )(any(), any())
        ).thenReturn(Future.successful(testIsaProductsAnswers))

        val result: Unit = service.update(testIsaProductsAnswers, testGroupId, testCredentials.providerId).futureValue

        result mustBe ()
        verify(mockDisaRegistrationConnector)
          .updateTaskListJourney(testIsaProductsAnswers, testGroupId, testIsaProductsAnswers.sectionName)
        verify(mockSessionRepository).upsertAndMarkUpdatesInSession(testCredentials.providerId)
      }

      "must still return the connector result when marking updates in session fails" in {
        when(mockSessionRepository.upsertAndMarkUpdatesInSession(any[String]))
          .thenReturn(Future.failed(new RuntimeException("fubar")))

        when(
          mockDisaRegistrationConnector.updateTaskListJourney(
            ArgumentMatchers.eq(testIsaProductsAnswers),
            ArgumentMatchers.eq(testGroupId),
            ArgumentMatchers.eq(testIsaProductsAnswers.sectionName)
          )(any(), any())
        ).thenReturn(Future.successful(testIsaProductsAnswers))

        val result = service.update(testIsaProductsAnswers, testGroupId, testCredentials.providerId).futureValue

        result mustBe testIsaProductsAnswers
        verify(mockDisaRegistrationConnector)
          .updateTaskListJourney(testIsaProductsAnswers, testGroupId, testIsaProductsAnswers.sectionName)
        verify(mockSessionRepository).upsertAndMarkUpdatesInSession(testCredentials.providerId)
      }

      "must propagate exception from connector" in {
        val ex = new Exception

        when(
          mockDisaRegistrationConnector.updateTaskListJourney(
            ArgumentMatchers.eq(testIsaProductsAnswers),
            ArgumentMatchers.eq(testGroupId),
            ArgumentMatchers.eq(testIsaProductsAnswers.sectionName)
          )(any(), any())
        ).thenReturn(Future.failed(ex))

        val thrown = service.update(testIsaProductsAnswers, testGroupId, testCredentials.providerId).failed.futureValue
        thrown mustBe ex

        verify(mockDisaRegistrationConnector)
          .updateTaskListJourney(testIsaProductsAnswers, testGroupId, testIsaProductsAnswers.sectionName)
        verify(mockSessionRepository, org.mockito.Mockito.never()).upsertAndMarkUpdatesInSession(any[String])
      }
    }

    "getOrCreateJourneyData" - {

      "must return GetOrCreateJourneyData when connector returns a response" in {
        val response = GetOrCreateJourneyData(
          isNewEnrolmentJourney = true,
          journeyData = testJourneyData
        )

        when(mockDisaRegistrationConnector.getOrCreateJourneyData(ArgumentMatchers.eq(testGroupId))(any()))
          .thenReturn(Future.successful(response))

        val result = service.getOrCreateJourneyData(testGroupId).futureValue

        result mustBe response
        verify(mockDisaRegistrationConnector).getOrCreateJourneyData(testGroupId)
      }

      "must propagate exception from connector" in {
        val ex = new Exception

        when(mockDisaRegistrationConnector.getOrCreateJourneyData(ArgumentMatchers.eq(testGroupId))(any()))
          .thenReturn(Future.failed(ex))

        val thrown = service.getOrCreateJourneyData(testGroupId).failed.futureValue

        thrown mustBe ex
      }
    }
  }
}
