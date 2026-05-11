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

package models.journeydata.orgdetails

import models.addresslookup.LookupAddress
import play.api.libs.json.*
import utils.JsonFormatSpec

class AddAnotherAddressJsonSpec extends JsonFormatSpec[AddAnotherAddress] {

  private val lookupAddress =
    LookupAddress(
      addressLine1 = Some("1 Test Street"),
      addressLine2 = Some("Line 2"),
      addressLine3 = Some("Town"),
      postCode = Some("AA1 1AA"),
      uprn = Some("123456789")
    )

  override val model: AddAnotherAddress =
    AddAnotherAddress(
      postcode = "AA1 1AA",
      filter = Some("Test Filter"),
      addresses = Seq(lookupAddress)
    )

  override val expectedJsonFromWrites: JsValue =
    Json.obj(
      "postcode"  -> "AA1 1AA",
      "filter"    -> "Test Filter",
      "addresses" -> Json.arr(
        Json.obj(
          "addressLine1" -> "1 Test Street",
          "addressLine2" -> "Line 2",
          "addressLine3" -> "Town",
          "postCode"     -> "AA1 1AA",
          "uprn"         -> "123456789"
        )
      )
    )

  override implicit val format: OFormat[AddAnotherAddress] =
    AddAnotherAddress.format
}