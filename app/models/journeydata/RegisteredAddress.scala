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

import play.api.libs.json.*

case class RegisteredAddress(
  addressLine1: Option[String] = None,
  addressLine2: Option[String] = None,
  addressLine3: Option[String] = None,
  postCode: Option[String] = None,
  uprn: Option[String] = None
)

object RegisteredAddress {

  val grsReads: Reads[RegisteredAddress] = for {
    addressLine1 <- (JsPath \ "address_line_1").readNullable[String]
    addressLine2 <- (JsPath \ "address_line_2").readNullable[String]
    addressLine3 <- (JsPath \ "locality").readNullable[String]
    postCode     <- (JsPath \ "postal_code").readNullable[String]
  } yield RegisteredAddress(addressLine1, addressLine2, addressLine3, postCode)

  implicit val format: OFormat[RegisteredAddress] = Json.format[RegisteredAddress]
}
