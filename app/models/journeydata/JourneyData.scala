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

package models.journeydata

import models.journeydata.certificatesofauthority.CertificatesOfAuthority
import models.journeydata.isaproducts.IsaProducts
import models.journeydata.liaisonofficers.LiaisonOfficers
import models.journeydata.signatories.Signatories
import models.journeydata.thirdparty.ThirdPartyOrganisations
import play.api.libs.json.{Format, Json, OWrites}

case class JourneyData(
  groupId: String,
  enrolmentId: String,
  businessVerification: Option[BusinessVerification] = None,
  organisationDetails: Option[OrganisationDetails] = None,
  isaProducts: Option[IsaProducts] = None,
  certificatesOfAuthority: Option[CertificatesOfAuthority] = None,
  liaisonOfficers: Option[LiaisonOfficers] = None,
  signatories: Option[Signatories] = None,
  thirdPartyOrganisations: Option[ThirdPartyOrganisations] = None
)

object JourneyData {
  implicit val format: Format[JourneyData] = Json.format[JourneyData]

  val auditWrites: OWrites[JourneyData] = OWrites[JourneyData] { jd =>
    Json.obj(
      "groupId"                      -> jd.groupId,
      "groupName"                    -> jd.businessVerification.flatMap(_.companyName).getOrElse("unknown"),
      "internalRegistrationId"       -> jd.enrolmentId,
      "organisationDetails"          -> jd.organisationDetails,
      "isaProducts"                  -> jd.isaProducts,
      "certificatesOfAuthority"      -> jd.certificatesOfAuthority,
      "liaisonOfficers"              -> jd.liaisonOfficers,
      "signatories"                  -> jd.signatories,
      "thirdPartyOrganisations" -> jd.thirdPartyOrganisations
    )
  }
}
