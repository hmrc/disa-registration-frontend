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
import models.grs.BusinessVerificationLockout
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes, UpdateOptions, Updates}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessVerificationLockoutRepository @Inject() (
  mongoComponent: MongoComponent,
  clock: Clock,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[BusinessVerificationLockout](
      mongoComponent = mongoComponent,
      collectionName = "BusinessVerificationLockouts",
      domainFormat = BusinessVerificationLockout.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("createdAt"),
          IndexOptions()
            .name("business-verification-lockout-expiry")
            .expireAfter(appConfig.bvLockoutTtl, TimeUnit.HOURS)
        ),
        IndexModel(
          Indexes.ascending("utr"),
          IndexOptions().unique(true)
        ),
        IndexModel(
          Indexes.ascending("groupId")
        )
      )
    ) {

  private def now(): Instant = Instant.now(clock)

  def lockOrg(groupId: String, utr: String): Future[Unit] =
    collection
      .updateOne(
        Filters.equal("utr", utr),
        Updates.combine(
          Updates.setOnInsert("utr", utr),
          Updates.setOnInsert("createdAt", now()),
          Updates.addToSet("groupId", groupId)
        ),
        UpdateOptions().upsert(true)
      )
      .toFuture()
      .map(_ => ())

  def isGroupLockedOut(groupId: String): Future[Boolean] =
    collection
      .countDocuments(
        Filters.equal("groupId", groupId)
      )
      .toFuture()
      .map(_ > 0)
}
