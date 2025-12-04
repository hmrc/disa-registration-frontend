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
import cats.data.EitherT
import connectors.DisaRegistrationConnector
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}

import scala.concurrent.Future

class JourneyAnswersServiceSpec extends SpecBase {

  val mockConnector: DisaRegistrationConnector =
    mock[DisaRegistrationConnector]

  val service = new JourneyAnswersService(mockConnector)

  private def rightT[A](resp: HttpResponse): EitherT[Future, UpstreamErrorResponse, HttpResponse] =
    EitherT.rightT[Future, UpstreamErrorResponse](resp)

  private def leftT[A](err: UpstreamErrorResponse): EitherT[Future, UpstreamErrorResponse, HttpResponse] =
    EitherT.leftT[Future, HttpResponse](err)

  "JourneyAnswersService" - {

    "get" - {

      "must return None when connector returns a Left (upstream error)" in {
        val upstreamError = UpstreamErrorResponse("", 500, 500)

        when(mockConnector.getJourneyData(ArgumentMatchers.eq(testGroupId))(any()))
          .thenReturn(leftT(upstreamError))

        val result = service.get(testGroupId).futureValue

        result mustBe None
      }

      "must return Some(JourneyData) when connector returns valid JSON in a Right" in {
        val json         = Json.toJson(testJourneyData)
        val httpResponse = HttpResponse(
          status = 200,
          body = json.toString()
        )

        when(mockConnector.getJourneyData(ArgumentMatchers.eq(testGroupId))(any()))
          .thenReturn(rightT(httpResponse))

        val result = service.get(testGroupId).futureValue

        result mustBe Some(testJourneyData)
      }

      "must return None when connector returns Right but JSON validation fails" in {
        val invalidJson  = Json.obj("something" -> "else")
        val httpResponse = HttpResponse(
          status = 200,
          body = invalidJson.toString()
        )

        when(mockConnector.getJourneyData(ArgumentMatchers.eq(testGroupId))(any()))
          .thenReturn(rightT(httpResponse))

        val result = service.get(testGroupId).futureValue

        result mustBe None
      }
    }

    "update" - {

      "must complete successfully when connector returns Right" in {
        val httpResponse = HttpResponse(200, "")

        when(
          mockConnector.updateTaskListJourney(
            ArgumentMatchers.eq(testIsaProductsAnswers),
            ArgumentMatchers.eq(testGroupId),
            ArgumentMatchers.eq(testIsaProductsAnswers.sectionName)
          )(any(), any())
        ).thenReturn(rightT(httpResponse))

        val result = service.update(testIsaProductsAnswers, testGroupId).futureValue

        result mustBe (())
        verify(mockConnector)
          .updateTaskListJourney(testIsaProductsAnswers, testGroupId, testIsaProductsAnswers.sectionName)
      }

      "must fail the Future when connector returns Left" in {
        val upstreamError = UpstreamErrorResponse("boom", 500, 500)

        when(
          mockConnector.updateTaskListJourney(
            ArgumentMatchers.eq(testIsaProductsAnswers),
            ArgumentMatchers.eq(testGroupId),
            ArgumentMatchers.eq(testIsaProductsAnswers.sectionName)
          )(any(), any())
        ).thenReturn(leftT(upstreamError))

        val ex = service.update(testIsaProductsAnswers, testGroupId).failed.futureValue

        ex mustBe a[Exception]
      }
    }
  }
}
