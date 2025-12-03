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

import connectors.DisaRegistrationConnector
import models.journeyData.JourneyData
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class JourneyAnswersService @Inject() (connector: DisaRegistrationConnector)(implicit ec: ExecutionContext) {

  def get(groupId: String)(implicit hc: HeaderCarrier): Future[Option[JourneyData]] =
    connector.getJourneyData(groupId).value.map {
      case Left(upstreamError) => ???
      case Right(response)     => response.json.validate[JourneyData].fold(_ => throw new Exception(), jd => Some(jd))
    }

  def update[A](journeyData: JourneyData, groupId: String, taskListJourney: String)(implicit
    hc: HeaderCarrier
  ): Future[Unit] =
    connector.updateJourneyData(journeyData, groupId, taskListJourney).value.map {
      case Left(upstreamError) => ???
      case Right(response)     => ()
    }
}
