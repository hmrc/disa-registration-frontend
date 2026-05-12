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

import models.journeydata.orgdetails.SelectedCorrespondenceAddress.{LookupAddress, ManualEntry}
import play.api.libs.json.{Format, JsValue, Json}
import utils.JsonFormatSpec

class SelectedCorrespondenceAddressSpec extends JsonFormatSpec[SelectedCorrespondenceAddress] {

  override def model: SelectedCorrespondenceAddress =
    LookupAddress(index = 1)

  override def expectedJsonFromWrites: JsValue =
    Json.obj(
      "type"  -> "lookup",
      "index" -> 1
    )

  override implicit def format: Format[SelectedCorrespondenceAddress] =
    SelectedCorrespondenceAddress.format

  "ManualEntry" - {

    "must serialise to JSON" in {
      Json.toJson[SelectedCorrespondenceAddress](ManualEntry) mustBe Json.obj(
        "type" -> "manual"
      )
    }

    "must deserialise from JSON" in {
      Json
        .obj("type" -> "manual")
        .as[SelectedCorrespondenceAddress] mustBe ManualEntry
    }
  }

  "LookupAddress" - {

    "must fail to deserialise when index is missing" in {
      Json
        .obj("type" -> "lookup")
        .validate[SelectedCorrespondenceAddress]
        .isError mustBe true
    }
  }

  "must fail to deserialise when type is unknown" in {
    Json
      .obj("type" -> "wibble")
      .validate[SelectedCorrespondenceAddress]
      .isError mustBe true
  }

  "must fail to deserialise when type is missing" in {
    Json
      .obj("index" -> 1)
      .validate[SelectedCorrespondenceAddress]
      .isError mustBe true
  }
}
