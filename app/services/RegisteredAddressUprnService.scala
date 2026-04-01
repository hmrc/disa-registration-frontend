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

import models.journeydata.{BusinessVerification, JourneyData, RegisteredAddress}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class RegisteredAddressUprnService @Inject() (
  addressLookupService: AddressLookupService,
  journeyAnswersService: JourneyAnswersService
)(implicit ec: ExecutionContext)
    extends Logging {

  def enrichUprnIfMissing(
    groupId: String,
    providerId: String,
    journeyDataOpt: Option[JourneyData]
  )(implicit hc: HeaderCarrier): Future[Unit] = {

    val journeyData = for {
      journeyData <- journeyDataOpt
      bv          <- journeyData.businessVerification
      address     <- bv.registeredAddress
    } yield (bv, address)

    journeyData match {

      case None =>
        logger.debug(s"No address available for enrichment for groupId: $groupId")
        Future.unit

      case Some((bv, address)) =>
        address.uprn match {
          case Some(_) =>
            logger.debug(s"UPRN already present for groupId: $groupId, skipping lookup")
            Future.unit
          case None    =>
            findAndPersist(address, bv, groupId, providerId)
        }
    }
  }.recover { case NonFatal(e) =>
    logger.error(s"Unexpected failure during UPRN enrichment for groupId: $groupId", e)
  }

  private def findAndPersist(
    address: RegisteredAddress,
    bv: BusinessVerification,
    groupId: String,
    providerId: String
  )(implicit hc: HeaderCarrier): Future[Unit] =
    addressLookupService
      .getUprn(address)
      .flatMap {
        case Some(uprn) =>
          logger.info(s"UPRN resolved for groupId: $groupId")

          val updatedAddress = address.copy(uprn = Some(uprn))
          val updatedBV      = bv.copy(registeredAddress = Some(updatedAddress))

          journeyAnswersService
            .update(updatedBV, groupId, providerId)
            .map(_ => logger.info(s"Registered address updated with UPRN for groupId: $groupId"))
            .recover { case NonFatal(e) =>
              logger.error(s"Failed to persist updated address for groupId: $groupId", e)
            }

        case None =>
          logger.info(
            s"Address lookup failed for groupId: $groupId"
          )
          Future.unit
      }
}
