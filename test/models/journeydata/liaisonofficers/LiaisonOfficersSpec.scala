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

package models.journeydata.liaisonofficers

import base.SpecBase
import models.journeydata.liaisonofficers.LiaisonOfficerCommunication.{ByEmail, ByPhone}

class LiaisonOfficersSpec extends SpecBase {

  "LiaisonOfficers" - {

    ".upsertLiaisonOfficer" - {

      "must update the full name when the liaison officer already exists" in {

        val existing =
          LiaisonOfficers(
            Seq(
              LiaisonOfficer(
                id = "existing-id",
                fullName = Some("Old Name"),
                phoneNumber = Some("0123456789"),
                email = Some("test@test.com"),
                communication = Set(ByEmail)
              )
            )
          )

        val result = existing.upsertLiaisonOfficer("existing-id", "Updated Name", max = 2)

        result mustEqual Some(
          LiaisonOfficers(
            Seq(
              LiaisonOfficer(
                id = "existing-id",
                fullName = Some("Updated Name"),
                phoneNumber = Some("0123456789"),
                email = Some("test@test.com"),
                communication = Set(ByEmail)
              )
            )
          )
        )
      }

      "must not modify other liaison officers when updating an existing one" in {

        val existing =
          LiaisonOfficers(
            Seq(
              LiaisonOfficer(
                id = "other-id",
                fullName = Some("Other Person"),
                phoneNumber = Some("0000000000"),
                email = Some("other@test.com"),
                communication = Set(ByPhone)
              ),
              LiaisonOfficer(
                id = "existing-id",
                fullName = Some("Old Name"),
                phoneNumber = Some("0123456789"),
                email = Some("test@test.com"),
                communication = Set(ByEmail)
              )
            )
          )

        val result = existing.upsertLiaisonOfficer("existing-id", "Updated Name", max = 3)

        result mustEqual Some(
          LiaisonOfficers(
            Seq(
              LiaisonOfficer(
                id = "other-id",
                fullName = Some("Other Person"),
                phoneNumber = Some("0000000000"),
                email = Some("other@test.com"),
                communication = Set(ByPhone)
              ),
              LiaisonOfficer(
                id = "existing-id",
                fullName = Some("Updated Name"),
                phoneNumber = Some("0123456789"),
                email = Some("test@test.com"),
                communication = Set(ByEmail)
              )
            )
          )
        )
      }

      "must add a new liaison officer when the id does not already exist and another can be added" in {

        val existing =
          LiaisonOfficers(
            Seq(
              LiaisonOfficer("other-id", Some("Other Person"))
            )
          )

        val result = existing.upsertLiaisonOfficer("new-id", "New Person", max = 2)

        result mustEqual Some(
          LiaisonOfficers(
            Seq(
              LiaisonOfficer("other-id", Some("Other Person")),
              LiaisonOfficer("new-id", Some("New Person"))
            )
          )
        )
      }

      "must add a new liaison officer with default fields when the id does not already exist" in {

        val existing = LiaisonOfficers(Seq.empty)

        val result = existing.upsertLiaisonOfficer("new-id", "New Person", max = 1)

        result mustEqual Some(
          LiaisonOfficers(
            Seq(
              LiaisonOfficer(
                id = "new-id",
                fullName = Some("New Person"),
                phoneNumber = None,
                email = None,
                communication = Set.empty
              )
            )
          )
        )
      }

      "must return None when the id does not already exist and the maximum has been reached" in {

        val existing =
          LiaisonOfficers(
            Seq(
              LiaisonOfficer("id-1", Some("Person 1")),
              LiaisonOfficer("id-2", Some("Person 2"))
            )
          )

        val result = existing.upsertLiaisonOfficer("new-id", "New Person", max = 2)

        result mustEqual None
      }

      "must still update an existing liaison officer when the maximum has been reached" in {

        val existing =
          LiaisonOfficers(
            Seq(
              LiaisonOfficer("id-1", Some("Person 1")),
              LiaisonOfficer("id-2", Some("Old Name"))
            )
          )

        val result = existing.upsertLiaisonOfficer("id-2", "Updated Name", max = 2)

        result mustEqual Some(
          LiaisonOfficers(
            Seq(
              LiaisonOfficer("id-1", Some("Person 1")),
              LiaisonOfficer("id-2", Some("Updated Name"))
            )
          )
        )
      }
    }

    ".canAddAnother" - {

      "must return true when the number of liaison officers is less than the maximum" in {

        val liaisonOfficers =
          LiaisonOfficers(
            Seq(
              LiaisonOfficer("id-1", Some("Person 1"))
            )
          )

        liaisonOfficers.canAddAnother(2) mustEqual true
      }

      "must return false when the number of liaison officers is equal to the maximum" in {

        val liaisonOfficers =
          LiaisonOfficers(
            Seq(
              LiaisonOfficer("id-1", Some("Person 1")),
              LiaisonOfficer("id-2", Some("Person 2"))
            )
          )

        liaisonOfficers.canAddAnother(2) mustEqual false
      }

      "must return false when the number of liaison officers is greater than the maximum" in {

        val liaisonOfficers =
          LiaisonOfficers(
            Seq(
              LiaisonOfficer("id-1", Some("Person 1")),
              LiaisonOfficer("id-2", Some("Person 2")),
              LiaisonOfficer("id-3", Some("Person 3"))
            )
          )

        liaisonOfficers.canAddAnother(2) mustEqual false
      }
    }
  }
}
