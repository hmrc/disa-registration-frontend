/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers

import controllers.actions.*
import models.grs.{BvPass, GRSResponse, RegisteredStatus}
import models.journeydata.BusinessVerification
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, RequestHeader}
import services.{GrsService, JourneyAnswersService}
import uk.gov.hmrc.http.{HeaderCarrier, RequestId}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class GrsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  journeyAnswersService: JourneyAnswersService,
  grsService: GrsService,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def grsCallback(journeyId: String): Action[AnyContent] =
    (identify andThen getData).async { implicit request =>
      val grsHc = buildGrsHeaderCarrier(request)

      grsService.fetchGRSJourneyData(journeyId)(grsHc).flatMap { grsResponse =>
        val businessVerification =
          buildBusinessVerification(grsResponse, request.journeyData.flatMap(_.businessVerification))
        journeyAnswersService.update(businessVerification, request.groupId).map { _ =>
          (businessVerification.businessRegistrationPassed, businessVerification.businessVerificationPassed) match {
            case (Some(true), Some(true)) =>
              Redirect(routes.TaskListController.onPageLoad()).withSession(request.session)

            case (_, Some(false)) =>
              Redirect(routes.BusinessVerificationController.lockout()).withSession(request.session)

            case _ =>
              Redirect(routes.StartController.onPageLoad()).withSession(request.session)
          }
        }
      }
    }

  private def buildGrsHeaderCarrier(request: RequestHeader): HeaderCarrier = {
    val baseHc = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    baseHc.copy(extraHeaders = request.headers.toSimpleMap.toSeq.filterNot(_._1.equalsIgnoreCase("Authorization")))
  }

  private def buildBusinessVerification(
    grs: GRSResponse,
    existing: Option[BusinessVerification]
  ): BusinessVerification = {
    val verificationPassed: Option[Boolean] =
      grs.businessVerificationStatus.map {
        case BvPass => true
        case _      => false
      }

    val registrationPassed: Option[Boolean] =
      Some(grs.businessRegistrationStatus == RegisteredStatus)

    existing
      .map(ev =>
        ev.copy(
          businessVerificationPassed = verificationPassed,
          businessRegistrationPassed = registrationPassed,
          ctUtr = grs.ctutr
        )
      )
      .getOrElse(
        BusinessVerification(
          businessVerificationPassed = verificationPassed,
          businessRegistrationPassed = registrationPassed,
          ctUtr = grs.ctutr
        )
      )
  }
}
