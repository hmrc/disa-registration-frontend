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

package models.journeydata.thirdparty

import base.SpecBase
import models.YesNoAnswer
import models.YesNoAnswer.Yes

class ThirdPartyOrganisationsSpec extends SpecBase {

  "ThirdPartyOrganisations" - {

    ".upsertThirdParty" - {

      "must update the name and frn when the third party already exists" in {

        val existing =
          ThirdPartyOrganisations(
            managedByThirdParty = None,
            thirdParties = Seq(
              ThirdParty(
                id = "existing-id",
                thirdPartyName = Some("Old Name"),
                thirdPartyFrn = Some("123456")
              )
            ),
            connectedOrganisations = Seq.empty
          )

        val result =
          existing.upsertThirdParty("existing-id", "Updated Name", Some("654321"))

        result mustEqual ThirdPartyOrganisations(
          managedByThirdParty = None,
          thirdParties = Seq(
            ThirdParty(
              id = "existing-id",
              thirdPartyName = Some("Updated Name"),
              thirdPartyFrn = Some("654321")
            )
          ),
          connectedOrganisations = Seq.empty
        )
      }

      "must not modify other third parties when updating an existing one" in {

        val existing =
          ThirdPartyOrganisations(
            managedByThirdParty = None,
            thirdParties = Seq(
              ThirdParty(
                id = "other-id",
                thirdPartyName = Some("Other Org"),
                thirdPartyFrn = Some("111111")
              ),
              ThirdParty(
                id = "existing-id",
                thirdPartyName = Some("Old Name"),
                thirdPartyFrn = Some("123456")
              )
            ),
            connectedOrganisations = Seq.empty
          )

        val result =
          existing.upsertThirdParty("existing-id", "Updated Name", Some("654321"))

        result mustEqual ThirdPartyOrganisations(
          managedByThirdParty = None,
          thirdParties = Seq(
            ThirdParty(
              id = "other-id",
              thirdPartyName = Some("Other Org"),
              thirdPartyFrn = Some("111111")
            ),
            ThirdParty(
              id = "existing-id",
              thirdPartyName = Some("Updated Name"),
              thirdPartyFrn = Some("654321")
            )
          ),
          connectedOrganisations = Seq.empty
        )
      }

      "must add a new third party when the id does not already exist" in {

        val existing =
          ThirdPartyOrganisations(
            managedByThirdParty = None,
            thirdParties = Seq(
              ThirdParty(
                id = "other-id",
                thirdPartyName = Some("Other Org")
              )
            ),
            connectedOrganisations = Seq.empty
          )

        val result =
          existing.upsertThirdParty("new-id", "New Org", Some("222222"))

        result mustEqual ThirdPartyOrganisations(
          managedByThirdParty = None,
          thirdParties = Seq(
            ThirdParty(
              id = "other-id",
              thirdPartyName = Some("Other Org")
            ),
            ThirdParty(
              id = "new-id",
              thirdPartyName = Some("New Org"),
              thirdPartyFrn = Some("222222")
            )
          ),
          connectedOrganisations = Seq.empty
        )
      }

      "must add a new third party with default fields when none exist" in {

        val existing =
          ThirdPartyOrganisations(
            managedByThirdParty = None,
            thirdParties = Seq.empty,
            connectedOrganisations = Seq.empty
          )

        val result =
          existing.upsertThirdParty("new-id", "New Org", None)

        result mustEqual ThirdPartyOrganisations(
          managedByThirdParty = None,
          thirdParties = Seq(
            ThirdParty(
              id = "new-id",
              thirdPartyName = Some("New Org"),
              thirdPartyFrn = None
            )
          ),
          connectedOrganisations = Seq.empty
        )
      }

      "must preserve all other fields when updating a third party" in {

        val existing =
          ThirdPartyOrganisations(
            managedByThirdParty = Some(Yes),
            thirdParties = Seq(
              ThirdParty(
                id = "existing-id",
                thirdPartyName = Some("Old Name"),
                thirdPartyFrn = Some("123456"),
                managingIsaReturns = Some(YesNoAnswer.Yes),
                usingInvestorFunds = Some(YesNoAnswer.Yes),
                investorFundsPercentage = Some("10")
              )
            ),
            connectedOrganisations = Seq("org-1", "org-2")
          )

        val result =
          existing.upsertThirdParty("existing-id", "Updated Name", Some("654321"))

        result.managedByThirdParty mustBe Some(Yes)
        result.connectedOrganisations mustBe Seq("org-1", "org-2")

        result.thirdParties.head mustBe ThirdParty(
          id = "existing-id",
          thirdPartyName = Some("Updated Name"),
          thirdPartyFrn = Some("654321"),
          managingIsaReturns = Some(YesNoAnswer.Yes),
          usingInvestorFunds = Some(YesNoAnswer.Yes),
          investorFundsPercentage = Some("10")
        )
      }
      "must update connectedOrganisations when an existing third party name changes" in {

        val existing =
          ThirdPartyOrganisations(
            managedByThirdParty = None,
            thirdParties = Seq(
              ThirdParty(
                id = "existing-id",
                thirdPartyName = Some("Old Name"),
                thirdPartyFrn = Some("123456")
              )
            ),
            connectedOrganisations = Seq("Old Name", "Another Org")
          )

        val result =
          existing.upsertThirdParty("existing-id", "Updated Name", Some("654321"))

        result.connectedOrganisations mustBe Seq(
          "Updated Name",
          "Another Org"
        )
      }

      "must not update connectedOrganisations when the third party name is unchanged" in {

        val existing =
          ThirdPartyOrganisations(
            managedByThirdParty = None,
            thirdParties = Seq(
              ThirdParty(
                id = "existing-id",
                thirdPartyName = Some("Same Name"),
                thirdPartyFrn = Some("123456")
              )
            ),
            connectedOrganisations = Seq("Same Name", "Another Org")
          )

        val result =
          existing.upsertThirdParty("existing-id", "Same Name", Some("654321"))

        result.connectedOrganisations mustBe Seq(
          "Same Name",
          "Another Org"
        )
      }

      "must not update connectedOrganisations when the existing third party has no name" in {

        val existing =
          ThirdPartyOrganisations(
            managedByThirdParty = None,
            thirdParties = Seq(
              ThirdParty(
                id = "existing-id",
                thirdPartyName = None,
                thirdPartyFrn = Some("123456")
              )
            ),
            connectedOrganisations = Seq("Another Org")
          )

        val result =
          existing.upsertThirdParty("existing-id", "Updated Name", Some("654321"))

        result.connectedOrganisations mustBe Seq("Another Org")
      }

      "must not modify connectedOrganisations when adding a new third party" in {

        val existing =
          ThirdPartyOrganisations(
            managedByThirdParty = None,
            thirdParties = Seq(
              ThirdParty(
                id = "other-id",
                thirdPartyName = Some("Other Org")
              )
            ),
            connectedOrganisations = Seq("Other Org")
          )

        val result =
          existing.upsertThirdParty("new-id", "New Org", Some("222222"))

        result.connectedOrganisations mustBe Seq("Other Org")
      }

      "must update only the matching connected organisation when multiple organisations exist" in {

        val existing =
          ThirdPartyOrganisations(
            managedByThirdParty = None,
            thirdParties = Seq(
              ThirdParty(
                id = "existing-id",
                thirdPartyName = Some("Old Name")
              )
            ),
            connectedOrganisations = Seq(
              "Org A",
              "Old Name",
              "Org B"
            )
          )

        val result =
          existing.upsertThirdParty("existing-id", "Updated Name", None)

        result.connectedOrganisations mustBe Seq(
          "Org A",
          "Updated Name",
          "Org B"
        )
      }
    }

    ".sectionName" - {

      "must return the correct section name" in {

        val model =
          ThirdPartyOrganisations(
            managedByThirdParty = None,
            thirdParties = Seq.empty,
            connectedOrganisations = Seq.empty
          )

        model.sectionName mustBe ThirdPartyOrganisations.sectionName
      }
    }

    "removeThirdParty" - {

      "must remove a third party and clear connectedOrganisations when only one third party remains" in {

        val model =
          ThirdPartyOrganisations(
            managedByThirdParty = None,
            thirdParties = Seq(
              ThirdParty(
                id = "tp-1",
                thirdPartyName = Some("Org A")
              ),
              ThirdParty(
                id = "tp-2",
                thirdPartyName = Some("Org B")
              )
            ),
            connectedOrganisations = Seq("Org A", "Org B", "Org C")
          )

        val result = model.removeThirdParty("tp-1")

        result.thirdParties mustBe Seq(
          ThirdParty(
            id = "tp-2",
            thirdPartyName = Some("Org B")
          )
        )
        result.connectedOrganisations mustBe Seq.empty
      }

      "must remove the third party and clear connectedOrganisations when only one remains, even if removed party has no name" in {

        val model =
          ThirdPartyOrganisations(
            managedByThirdParty = None,
            thirdParties = Seq(
              ThirdParty(
                id = "tp-1",
                thirdPartyName = None
              ),
              ThirdParty(
                id = "tp-2",
                thirdPartyName = Some("Org B")
              )
            ),
            connectedOrganisations = Seq("Org B")
          )

        val result = model.removeThirdParty("tp-1")

        result.thirdParties mustBe Seq(
          ThirdParty(
            id = "tp-2",
            thirdPartyName = Some("Org B")
          )
        )

        result.connectedOrganisations mustBe Seq.empty
      }

      "must do nothing when third party id does not exist" in {

        val model =
          ThirdPartyOrganisations(
            managedByThirdParty = None,
            thirdParties = Seq(
              ThirdParty(
                id = "tp-1",
                thirdPartyName = Some("Org A")
              )
            ),
            connectedOrganisations = Seq.empty
          )

        val result = model.removeThirdParty("missing-id")

        result mustBe model
      }

      "must handle empty state safely" in {

        val model =
          ThirdPartyOrganisations(
            managedByThirdParty = None,
            thirdParties = Seq.empty,
            connectedOrganisations = Seq.empty
          )

        val result = model.removeThirdParty("anything")

        result mustBe model
      }

      "must clear connectedOrganisations when removing a third party leaves only one remaining" in {

        val model =
          ThirdPartyOrganisations(
            managedByThirdParty = None,
            thirdParties = Seq(
              ThirdParty(
                id = "tp-1",
                thirdPartyName = Some("Org A")
              ),
              ThirdParty(
                id = "tp-2",
                thirdPartyName = Some("Org B")
              )
            ),
            connectedOrganisations = Seq("Org A", "Org B")
          )

        val result = model.removeThirdParty("tp-1")

        result.thirdParties mustBe Seq(
          ThirdParty(
            id = "tp-2",
            thirdPartyName = Some("Org B")
          )
        )

        result.connectedOrganisations mustBe Seq.empty
      }
    }
  }
}
