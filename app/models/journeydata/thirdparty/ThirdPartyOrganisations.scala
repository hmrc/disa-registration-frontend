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

import models.YesNoAnswer
import models.journeydata.TaskListSection
import play.api.libs.json.{Json, OFormat}

case class ThirdPartyOrganisations(
  managedByThirdParty: Option[YesNoAnswer] = None,
  thirdParties: Seq[ThirdParty] = Nil,
  connectedOrganisations: Set[String] = Set.empty
) extends TaskListSection {
  def sectionName: String = ThirdPartyOrganisations.sectionName

  def upsertThirdParty(id: String, name: String, frn: Option[String]): ThirdPartyOrganisations = {
    val exists = thirdParties.exists(_.id == id)

    val updated =
      if (exists)
        thirdParties.map {
          case tp if tp.id == id =>
            tp.copy(
              thirdPartyName = Some(name),
              thirdPartyFrn = frn
            )
          case tp                => tp
        }
      else
        thirdParties :+ ThirdParty(
          id = id,
          thirdPartyName = Some(name),
          thirdPartyFrn = frn
        )

    copy(thirdParties = updated)
  }
}

object ThirdPartyOrganisations {
  val sectionName: String                               = "thirdPartyOrganisations"
  implicit val format: OFormat[ThirdPartyOrganisations] = Json.format[ThirdPartyOrganisations]
}
