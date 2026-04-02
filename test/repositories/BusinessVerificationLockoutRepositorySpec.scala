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
import org.mongodb.scala.model.Filters
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.mongo.MongoComponent
import org.mongodb.scala.*
import play.api.{Application, inject}

import java.time.{Clock, Instant, ZoneOffset}

class BusinessVerificationLockoutRepositorySpec extends SpecBase {

  private val fixedInstant = Instant.parse("2025-01-01T00:00:00Z")
  private val clock: Clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

  protected val databaseName: String = "disa-session-repository-test"
  protected val mongoUri: String     = s"mongodb://127.0.0.1:27017/$databaseName"

  lazy val mongoComponent: MongoComponent =
    MongoComponent(mongoUri)

  private val repository: BusinessVerificationLockoutRepository =
    new BusinessVerificationLockoutRepository(mongoComponent, clock, mockAppConfig)

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repository.collection.drop().toFuture())
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(repository.collection.drop().toFuture())
  }

  "BusinessVerificationLockoutRepository" - {

    "return false when no lockout exists" in {
      repository.isLockedOut("user-1").futureValue mustBe false
    }

    "lock a user and return true when checked" in {
      repository.lockUser("user-1").futureValue

      repository.isLockedOut("user-1").futureValue mustBe true
    }

    "upsert a lockout when user already exists" in {
      repository.lockUser("user-1").futureValue
      repository.lockUser("user-1").futureValue

      repository.isLockedOut("user-1").futureValue mustBe true

      val count = await(
        repository.collection.find(Filters.equal("userId", "user-1")).toFuture()
      ).size

      count mustBe 1
    }

    "store createdAt when locking a user" in {

      val app: Application = applicationBuilder(
        journeyData = None,
        overrides = Seq(
          inject.bind[Clock].toInstance(clock)
        )
      ).build()

      repository.lockUser("user-2").futureValue

      val record = await(
        repository.collection.find(Filters.equal("userId", "user-2")).head()
      )

      record.createdAt mustBe fixedInstant
      record.userId mustBe "user-2"
    }

    "handle multiple users independently" in {
      repository.lockUser("user-1").futureValue
      repository.lockUser("user-2").futureValue

      repository.isLockedOut("user-1").futureValue mustBe true
      repository.isLockedOut("user-2").futureValue mustBe true
      repository.isLockedOut("user-3").futureValue mustBe false
    }
  }
}
