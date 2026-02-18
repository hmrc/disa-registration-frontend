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

package repositories

import config.FrontendAppConfig
import models.session.Session
import org.bson.conversions.Bson
import org.mongodb.scala.model.*
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionRepository @Inject() (
  mongoComponent: MongoComponent,
  appConfig: FrontendAppConfig,
  clock: Clock
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[Session](
      collectionName = "sessions",
      mongoComponent = mongoComponent,
      domainFormat = Session.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("lastUpdated"),
          IndexOptions()
            .name("lastUpdatedIdx")
            .expireAfter(appConfig.cacheTtl, TimeUnit.SECONDS)
        ),
        IndexModel(
          Indexes.ascending("userId"),
          IndexOptions()
            .name("userIdIdx")
            .unique(true)
        )
      ),
      replaceIndexes = true
    ) {

  private def byId(id: String): Bson = Filters.equal("userId", id)

  def getOrCreateSessionAndMarkAuditEventSent(userId: String): Future[Boolean] =
    val now = Instant.now(clock)
    collection
      .findOneAndUpdate(
        filter = byId(userId),
        update = Updates.combine(
          Updates.set("auditContinuationEventSent", true),
          Updates.set("lastUpdated", now),
          Updates.setOnInsert("userId", userId)
        ),
        options = new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.BEFORE)
      )
      .toFutureOption()
      .map {
        case None           => true
        case Some(existing) => !existing.auditContinuationEventSent
      }

  def keepAlive(userId: String): Future[Unit] =
    collection
      .updateOne(
        filter = byId(userId),
        update = Updates.set("lastUpdated", Instant.now(clock))
      )
      .toFuture()
      .map(_ => ())
}
