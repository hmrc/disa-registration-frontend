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
import models.GetOrCreateJourneyData
import models.journeydata.{JourneyData, TaskListSection}
import play.api.Logging
import play.api.libs.json.Writes
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class JourneyAnswersService @Inject() (connector: DisaRegistrationConnector, sessionRepository: SessionRepository)(
  implicit executionContext: ExecutionContext
) extends Logging {

  def get(groupId: String)(implicit hc: HeaderCarrier): Future[Option[JourneyData]] =
    connector.getJourneyData(groupId)

  def update[A <: TaskListSection: Writes](taskListSection: A, groupId: String, userId: String)(implicit
    hc: HeaderCarrier
  ): Future[A] = {
    val sectionName = taskListSection.sectionName
    connector
      .updateTaskListJourney(taskListSection, groupId, sectionName)
      .flatMap { result =>
        sessionRepository
          .upsertAndMarkUpdatesInSession(userId)
          .recover { case e => logger.warn(s"Failed to mark updates in session for userId: [$userId]", e) }
          .map(_ => result)
      }
  }

  def getOrCreateJourneyData(groupId: String)(implicit hc: HeaderCarrier): Future[GetOrCreateJourneyData] =
    connector.getOrCreateJourneyData(groupId)
}
