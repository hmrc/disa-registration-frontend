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

package models.grs

import models.{Enumerable, WithName}

sealed trait GrsIdentificationService {
  def apiBasePath: String
}

object GrsIdentificationService {

  case object IncorporatedEntityIdentification extends GrsIdentificationService {
    val apiBasePath = "incorporated-entity-identification/api"
  }

  case object PartnershipIdentification extends GrsIdentificationService {
    val apiBasePath = "partnership-identification/api"
  }
}

sealed trait GrsCompanyType {
  def createJourneyPath: String
  def identificationService: GrsIdentificationService
}

object GrsCompanyType extends Enumerable.Implicits {

  case object LimitedCompany extends WithName("limitedCompany") with GrsCompanyType {
    val createJourneyPath    = "limited-company-journey"
    val identificationService = GrsIdentificationService.IncorporatedEntityIdentification
  }

  // Not a distinct GRS journey - maps to the Limited Company journey, since a European
  // Institution with a UK Base has no dedicated business entity type in GRS.
  case object EuropeanInstitutionWithAUkBase
      extends WithName("europeanInstitutionWithAUkBase")
      with GrsCompanyType {
    val createJourneyPath    = "limited-company-journey"
    val identificationService = GrsIdentificationService.IncorporatedEntityIdentification
  }

  case object IncorporatedFriendlySociety extends WithName("incorporatedFriendlySociety") with GrsCompanyType {
    val createJourneyPath    = "registered-society-journey"
    val identificationService = GrsIdentificationService.IncorporatedEntityIdentification
  }

  case object RegisteredFriendlySociety extends WithName("registeredFriendlySociety") with GrsCompanyType {
    val createJourneyPath    = "registered-society-journey"
    val identificationService = GrsIdentificationService.IncorporatedEntityIdentification
  }

  case object GeneralPartnership extends WithName("generalPartnership") with GrsCompanyType {
    val createJourneyPath    = "general-partnership-journey"
    val identificationService = GrsIdentificationService.PartnershipIdentification
  }

  case object ScottishPartnership extends WithName("scottishPartnership") with GrsCompanyType {
    val createJourneyPath    = "scottish-partnership-journey"
    val identificationService = GrsIdentificationService.PartnershipIdentification
  }

  case object ScottishLimitedPartnership extends WithName("scottishLimitedPartnership") with GrsCompanyType {
    val createJourneyPath    = "scottish-limited-partnership-journey"
    val identificationService = GrsIdentificationService.PartnershipIdentification
  }

  case object LimitedPartnership extends WithName("limitedPartnership") with GrsCompanyType {
    val createJourneyPath    = "limited-partnership-journey"
    val identificationService = GrsIdentificationService.PartnershipIdentification
  }

  case object LimitedLiabilityPartnership extends WithName("limitedLiabilityPartnership") with GrsCompanyType {
    val createJourneyPath    = "limited-liability-partnership-journey"
    val identificationService = GrsIdentificationService.PartnershipIdentification
  }

  val values: Seq[GrsCompanyType] = Seq(
    LimitedCompany,
    EuropeanInstitutionWithAUkBase,
    IncorporatedFriendlySociety,
    RegisteredFriendlySociety,
    GeneralPartnership,
    ScottishPartnership,
    ScottishLimitedPartnership,
    LimitedPartnership,
    LimitedLiabilityPartnership
  )

  implicit val enumerable: Enumerable[GrsCompanyType] =
    Enumerable(values.map(v => v.toString -> v)*)
}
