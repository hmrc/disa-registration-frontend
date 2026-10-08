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

import models.YesNoAnswer
import models.journeydata.orgdetails.AddAnotherAddress
import play.api.libs.json.{Json, OFormat}

case class OrganisationDetails(
  registeredToManageIsa: Option[YesNoAnswer] = None,
  zRefNumber: Option[String] = None,
  tradingUsingDifferentName: Option[YesNoAnswer] = None,
  tradingName: Option[String] = None,
  fcaNumber: Option[String] = None,
  registeredAddressCorrespondence: Option[YesNoAnswer] = None,
  correspondenceAddress: Option[CorrespondenceAddress] = None,
  orgTelephoneNumber: Option[String] = None,
  addAnotherAddress: Option[AddAnotherAddress] = None
) extends TaskListSection {
  override def sectionName: String = OrganisationDetails.sectionName

  def hasSelectedCorrespondenceAddress: Boolean =
    correspondenceAddress.exists(_.isPopulated)

  def hasNoSelectedCorrespondenceAddress: Boolean =
    !hasSelectedCorrespondenceAddress

  def isComplete: Boolean =
    registeredToManageIsa.isDefined &&
      zReferenceComplete &&
      tradingNameComplete &&
      fcaNumber.exists(_.nonEmpty) &&
      correspondenceAddressComplete &&
      orgTelephoneNumber.exists(_.nonEmpty)

  private def zReferenceComplete: Boolean =
    registeredToManageIsa.exists {
      case YesNoAnswer.No  => true
      case YesNoAnswer.Yes => zRefNumber.exists(_.nonEmpty)
    }

  private def tradingNameComplete: Boolean =
    tradingUsingDifferentName.exists {
      case YesNoAnswer.Yes => tradingName.exists(_.nonEmpty)
      case YesNoAnswer.No  => true
    }

  private def correspondenceAddressComplete: Boolean =
    registeredAddressCorrespondence.exists {
      case YesNoAnswer.Yes => true
      case YesNoAnswer.No  => hasSelectedCorrespondenceAddress
    }
}

object OrganisationDetails {
  val sectionName = "organisationDetails"

  implicit val format: OFormat[OrganisationDetails] = Json.format[OrganisationDetails]

}
