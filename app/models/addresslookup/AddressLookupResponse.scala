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

package models.addresslookup

import play.api.libs.json.{Json, OFormat}

case class AddressLookupResponse(
  address: AddressLookupAddress,
  uprn: Option[Long]
) {

  def toLookupAddress: LookupAddress =
    LookupAddress(
      addressLine1 = address.lines.headOption,
      addressLine2 = address.lines.lift(1),
      addressLine3 = address.town,
      postCode = address.postcode,
      uprn = uprn.map(_.toString),
      country = address.country.map(_.name)
    )
}

object AddressLookupResponse {
  implicit val format: OFormat[AddressLookupResponse] =
    Json.format[AddressLookupResponse]
}

case class AddressLookupAddress(
  lines: Seq[String],
  town: Option[String],
  postcode: Option[String],
  country: Option[AddressLookupCountry]
)

object AddressLookupAddress {
  implicit val format: OFormat[AddressLookupAddress] =
    Json.format[AddressLookupAddress]
}

case class AddressLookupCountry(
  code: String,
  name: String
)

object AddressLookupCountry {
  implicit val format: OFormat[AddressLookupCountry] =
    Json.format[AddressLookupCountry]
}
