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

package services

import connectors.AddressLookupConnector
import models.journeydata.RegisteredAddress
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddressLookupService @Inject() (
                                       connector: AddressLookupConnector
                                     )(implicit ec: ExecutionContext) {

  private val DefaultUprn = "100000000000"

  def getUprn(address: RegisteredAddress)(implicit hc: HeaderCarrier): Future[String] = {

    val postcode = address.postCode.getOrElse(
      throw new IllegalArgumentException("Postcode is required for address lookup")
    )

    val filter = address.addressLine1

    connector.searchAddress(postcode, filter).map { json =>
      val uprnOpt =
        (json \ "addresses")
          .asOpt[Seq[JsValue]]
          .flatMap(_.headOption)
          .flatMap(addr => (addr \ "uprn").asOpt[String])

      uprnOpt.getOrElse(DefaultUprn)
    }
  }
}