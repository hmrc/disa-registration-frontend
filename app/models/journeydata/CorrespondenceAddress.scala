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

package models.journeydata

import models.addresslookup.LookupAddress
import play.api.libs.json._

case class CorrespondenceAddress(
  addressLine1: Option[String] = None,
  addressLine2: Option[String] = None,
  addressLine3: Option[String] = None,
  postCode: Option[String] = None
) {
  def isPopulated: Boolean =
    List(addressLine1, addressLine3, postCode).exists(_.isDefined)
}

object CorrespondenceAddress {

  implicit val format: OFormat[CorrespondenceAddress] = Json.format[CorrespondenceAddress]

  def fromLookup(address: LookupAddress): CorrespondenceAddress =
    CorrespondenceAddress(
      addressLine1 = address.addressLine1,
      addressLine2 = address.addressLine2,
      addressLine3 = address.addressLine3,
      postCode = address.postCode
    )

  def matches(selected: CorrespondenceAddress, lookup: LookupAddress): Boolean =
    selected.addressLine1 == lookup.addressLine1 &&
      selected.addressLine2 == lookup.addressLine2 &&
      selected.addressLine3 == lookup.addressLine3 &&
      selected.postCode == lookup.postCode
}
