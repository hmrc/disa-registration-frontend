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
  connectedOrganisations: Seq[String] = Nil
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

  def completedThirdParties: Seq[ThirdParty] =
    thirdParties.filterNot(_.inProgress)

  def completedCount: Int =
    completedThirdParties.size

  def hasMultipleCompleted: Boolean =
    completedCount > 1

  def hasSingleCompleted: Boolean =
    completedCount == 1

  def canAccessCheckYourAnswers: Boolean =
    hasMultipleCompleted

  def removeThirdParty(id: String): ThirdPartyOrganisations = {

    val removedThirdPartyOpt =
      thirdParties.find(_.id == id)

    val updatedThirdParties =
      thirdParties.filterNot(_.id == id)

    val connectedAfterNameRemoval =
      removedThirdPartyOpt
        .flatMap(_.thirdPartyName)
        .map { name =>
          connectedOrganisations.filterNot(_ == name)
        }
        .getOrElse(connectedOrganisations)

    val finalConnectedOrgs =
      if (updatedThirdParties.size == 1) Nil
      else connectedAfterNameRemoval

    copy(
      thirdParties = updatedThirdParties,
      connectedOrganisations = finalConnectedOrgs
    )
  }
}

object ThirdPartyOrganisations {
  val sectionName: String                               = "thirdPartyOrganisations"
  implicit val format: OFormat[ThirdPartyOrganisations] = Json.format[ThirdPartyOrganisations]
}
