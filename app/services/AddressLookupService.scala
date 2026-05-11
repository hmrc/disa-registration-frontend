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
import play.api.Logging
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import config.Constants.defaultUprn
import models.addresslookup.LookupAddress

class AddressLookupService @Inject() (
  connector: AddressLookupConnector
)(implicit ec: ExecutionContext)
    extends Logging {

  def lookup(postcode: String, filter: Option[String])
            (implicit hc: HeaderCarrier): Future[Seq[LookupAddress]] = {

    connector.searchAddress(postcode, filter)
      .recover { case NonFatal(e) =>
        logger.warn(s"Address lookup failed for $postcode", e)
        Seq.empty
      }
  }

  def getUprn(address: LookupAddress)
             (implicit hc: HeaderCarrier): Future[Option[String]] = {

    address.uprn match {

      case some @ Some(_) =>
        Future.successful(some)

      case None =>
        logger.warn(
          s"No UPRN returned for selected address, defaulting. Postcode=${address.postcode}"
        )
        Future.successful(Some(config.Constants.defaultUprn))
    }
  }
}
