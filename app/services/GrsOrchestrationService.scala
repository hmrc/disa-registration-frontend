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

import models.grs.GRSResponse
import models.journeydata.{BusinessVerification, RegisteredAddress}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GrsOrchestrationService @Inject() (
                                          grsService: GrsService,
                                          addressLookupService: AddressLookupService,
                                          journeyAnswersService: JourneyAnswersService
                                        )(implicit ec: ExecutionContext)
  extends Logging {

  def processGrsJourney(
                         journeyId: String,
                         existing: Option[BusinessVerification],
                         groupId: String,
                         providerId: String
                       )(implicit hc: HeaderCarrier): Future[BusinessVerification] = {

    grsService.fetchGRSJourneyData(journeyId).flatMap { grsResponse =>

      val businessVerification =
        buildBusinessVerification(grsResponse, existing)

      enrichAddressIfEligible(grsResponse, businessVerification).flatMap { finalAddress =>

        val updated =
          businessVerification.copy(
            registeredAddress = finalAddress
          )

        journeyAnswersService
          .update(updated, groupId, providerId)
          .map(_ => updated)
      }
    }
  }

  private def enrichAddressIfEligible(
                                       grsResponse: GRSResponse,
                                       bv: BusinessVerification
                                     )(implicit hc: HeaderCarrier): Future[Option[RegisteredAddress]] = {

    val isPassed =
      (bv.businessRegistrationPassed, bv.businessVerificationPassed) match {
        case (Some(true), Some(true)) => true
        case _ => false
      }

    if (!isPassed) {
      Future.successful(None)
    } else {
      grsResponse.registeredAddress match {

        case Some(address) =>
          addressLookupService.getUprn(address).map { uprn =>
            Some(address.copy(uprn = Some(uprn)))
          }.recoverWith {
            case e =>
              logger.error(
                s"Address lookup failed for registered address: $address",
                e
              )
              Future.failed(new RuntimeException(
                s"Address lookup failed for postcode ${address.postCode.getOrElse("unknown")}",
                e
              ))
          }

        case None =>
          logger.error("GRS/BV passed but no registered address returned")
          Future.failed(new RuntimeException("Missing address after successful GRS/BV"))
      }
    }
  }


  private def buildBusinessVerification(
                                         grs: GRSResponse,
                                         existing: Option[BusinessVerification]
                                       ): BusinessVerification = {

    val verificationPassed =
      grs.businessVerificationStatus.map(_ == models.grs.BvPass)

    val registrationPassed =
      Some(grs.businessRegistrationStatus == models.grs.RegisteredStatus)

    existing
      .map(_.copy(
        businessVerificationPassed = verificationPassed,
        businessRegistrationPassed = registrationPassed,
        ctUtr = grs.ctutr,
        registeredAddress = grs.registeredAddress
      ))
      .getOrElse(
        BusinessVerification(
          businessVerificationPassed = verificationPassed,
          businessRegistrationPassed = registrationPassed,
          ctUtr = grs.ctutr,
          registeredAddress = grs.registeredAddress
        )
      )
  }
}