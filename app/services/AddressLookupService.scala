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

class AddressLookupService @Inject() (
  connector: AddressLookupConnector
)(implicit ec: ExecutionContext)
    extends Logging {

  private val DefaultUprn = "100000000000"

  def getUprn(address: RegisteredAddress)(implicit hc: HeaderCarrier): Future[Option[String]] = {

    val postcode = address.postCode.getOrElse {
      val message = s"Postcode is required for address lookup but was missing. Address: $address"
      logger.error(message)
      throw new IllegalArgumentException(message)
    }

    val filter = address.addressLine1

    connector
      .searchAddress(postcode, filter)
      .map { json =>
        extractUprn(json) match {
          case Some(uprn) =>
            logger.info(s"UPRN found for postcode: $postcode")
            Some(uprn)

          case None =>
            logger.warn(
              s"No UPRN found in lookup response, using default. Postcode: $postcode, filter: $filter"
            )
            Some(DefaultUprn)
        }
      }
      .recover { case NonFatal(e) =>
        logger.warn(
          s"Address lookup failed for postcode: $postcode, filter: $filter",
          e
        )
        None
      }
  }

  private def extractUprn(json: JsValue): Option[String] =
    (json \ "addresses")
      .asOpt[Seq[JsValue]]
      .flatMap(_.headOption)
      .flatMap(addr => (addr \ "uprn").asOpt[String])
}
