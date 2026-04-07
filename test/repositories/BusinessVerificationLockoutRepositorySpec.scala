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
import java.time.{Clock, Instant, ZoneOffset}

class BusinessVerificationLockoutRepositorySpec extends SpecBase {

  private val fixedInstant = Instant.parse("2025-01-01T00:00:00Z")
  private val clock: Clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

  protected val databaseName: String = "disa-registration-frontend-test"
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
      repository.isGroupLockedOut("group-1").futureValue mustBe false
    }

    "lock a groupId under a ctutr and return true when checked" in {
      repository.lockOrg("group-1", "ctutr-1").futureValue

      repository.isGroupLockedOut("group-1").futureValue mustBe true
    }

    "upsert a lockout when ctutr already exists and append groupId" in {
      repository.lockOrg("group-1", "ctutr-1").futureValue
      repository.lockOrg("group-2", "ctutr-1").futureValue

      repository.isGroupLockedOut("group-1").futureValue mustBe true
      repository.isGroupLockedOut("group-2").futureValue mustBe true

      val record = await(
        repository.collection.find(Filters.equal("ctutr", "ctutr-1")).head()
      )

      record.groupId must contain allOf ("group-1", "group-2")
      record.groupId.size mustBe 2
    }

    "not duplicate groupIds when locking same group multiple times" in {
      repository.lockOrg("group-1", "ctutr-1").futureValue
      repository.lockOrg("group-1", "ctutr-1").futureValue

      val record = await(
        repository.collection.find(Filters.equal("ctutr", "ctutr-1")).head()
      )

      record.groupId.count(_ == "group-1") mustBe 1
    }

    "store createdAt when locking a group" in {
      repository.lockOrg("group-2", "ctutr-2").futureValue

      val record = await(
        repository.collection.find(Filters.equal("ctutr", "ctutr-2")).head()
      )

      record.createdAt mustBe fixedInstant
      record.groupId must contain("group-2")
    }

    "handle multiple groups across different ctutrs independently" in {
      repository.lockOrg("group-1", "ctutr-1").futureValue
      repository.lockOrg("group-2", "ctutr-2").futureValue

      repository.isGroupLockedOut("group-1").futureValue mustBe true
      repository.isGroupLockedOut("group-2").futureValue mustBe true
      repository.isGroupLockedOut("group-3").futureValue mustBe false
    }

    "allow multiple groupIds under the same ctutr" in {
      repository.lockOrg("group-1", "ctutr-1").futureValue
      repository.lockOrg("group-2", "ctutr-1").futureValue
      repository.lockOrg("group-3", "ctutr-1").futureValue

      val record = await(
        repository.collection.find(Filters.equal("ctutr", "ctutr-1")).head()
      )

      record.groupId must contain allOf ("group-1", "group-2", "group-3")
      record.groupId.size mustBe 3
    }
  }
}
