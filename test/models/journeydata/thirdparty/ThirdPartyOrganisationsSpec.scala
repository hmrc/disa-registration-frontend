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
            connectedOrganisations = Set.empty
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
          connectedOrganisations = Set.empty
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
            connectedOrganisations = Set.empty
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
          connectedOrganisations = Set.empty
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
            connectedOrganisations = Set.empty
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
          connectedOrganisations = Set.empty
        )
      }

      "must add a new third party with default fields when none exist" in {

        val existing =
          ThirdPartyOrganisations(
            managedByThirdParty = None,
            thirdParties = Seq.empty,
            connectedOrganisations = Set.empty
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
          connectedOrganisations = Set.empty
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
                usingInvestorFunds = Some(false),
                investorFundsPercentage = Some(10)
              )
            ),
            connectedOrganisations = Set("org-1", "org-2")
          )

        val result =
          existing.upsertThirdParty("existing-id", "Updated Name", Some("654321"))

        result.managedByThirdParty mustBe Some(Yes)
        result.connectedOrganisations mustBe Set("org-1", "org-2")

        result.thirdParties.head mustBe ThirdParty(
          id = "existing-id",
          thirdPartyName = Some("Updated Name"),
          thirdPartyFrn = Some("654321"),
          managingIsaReturns = Some(YesNoAnswer.Yes),
          usingInvestorFunds = Some(false),
          investorFundsPercentage = Some(10)
        )
      }
    }
    ".sectionName" - {

      "must return the correct section name" in {

        val model =
          ThirdPartyOrganisations(
            managedByThirdParty = None,
            thirdParties = Seq.empty,
            connectedOrganisations = Set.empty
          )

        model.sectionName mustBe ThirdPartyOrganisations.sectionName
      }
    }
  }
}
