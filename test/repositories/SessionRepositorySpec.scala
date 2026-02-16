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

import base.SpecBase
import models.session.Session
import org.mongodb.scala.ObservableFuture
import org.mongodb.scala.model.Filters
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.mongo.MongoComponent

import java.time.{Clock, Instant, ZoneOffset}

class SessionRepositorySpec extends SpecBase {

  protected val databaseName: String          = "disa-session-repository-test"
  protected val mongoUri: String              = s"mongodb://127.0.0.1:27017/$databaseName"
  lazy val mockMongoComponent: MongoComponent = MongoComponent(mongoUri)

  private val fixedNow: Instant = Instant.parse("2026-02-16T10:00:00Z")
  private val fixedClock: Clock = Clock.fixed(fixedNow, ZoneOffset.UTC)

  private val repository: SessionRepository =
    new SessionRepository(mockMongoComponent, mockAppConfig, fixedClock)

  override def beforeEach(): Unit =
    await(repository.collection.drop().toFuture())

  override def afterAll(): Unit = {
    super.afterAll()
    await(repository.collection.drop().toFuture())
  }

  private def buildSession(
    groupId: String,
    sent: Boolean = false,
    lastUpdated: Instant = Instant.parse("2026-02-16T09:00:00Z")
  ) =
    Session(
      groupId = groupId,
      auditContinuationEventSent = sent,
      lastUpdated = lastUpdated
    )

  "markAuditEventSent" - {

    "return true and set auditContinuationEventSent to true when it was previously false" in {
      val session = buildSession(testGroupId, sent = false)
      await(repository.collection.insertOne(session).toFuture())

      val res = await(repository.markAuditEventSent(testGroupId))
      res mustBe true

      val stored = await(repository.collection.find(Filters.eq("groupId", testGroupId)).head())
      stored.auditContinuationEventSent mustBe true
    }

    "return false when auditContinuationEventSent is already true" in {
      val session = buildSession(testGroupId, sent = true)
      await(repository.collection.insertOne(session).toFuture())

      val res = await(repository.markAuditEventSent(testGroupId))
      res mustBe false

      val stored = await(repository.collection.find(Filters.eq("groupId", testGroupId)).head())
      stored.auditContinuationEventSent mustBe true
    }

    "return false when no document exists for the groupId" in {
      val res = await(repository.markAuditEventSent(testGroupId))
      res mustBe false
    }
  }

  "keepAlive" - {

    "update lastUpdated to the current clock time and return unit when a document exists" in {

      val session = buildSession(testGroupId, sent = false, lastUpdated = Instant.parse("2026-02-16T08:00:00Z"))
      await(repository.collection.insertOne(session).toFuture())

      val res = await(repository.keepAlive(testGroupId))
      res mustBe ()

      val stored = await(repository.collection.find(Filters.eq("groupId", testGroupId)).head())
      stored.lastUpdated mustBe fixedNow
    }

    "return unit even when no document exists for the groupId" in {
      val res = await(repository.keepAlive(testGroupId))
      res mustBe ()
    }
  }
}
