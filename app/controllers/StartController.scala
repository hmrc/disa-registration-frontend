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

package controllers

import controllers.actions.*
import models.grs.GrsCompanyType
import models.journeydata.BusinessVerification
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{BusinessVerificationLockoutService, GrsService, JourneyAnswersService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class StartController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getOrCreateJourneyData: GetOrCreateJourneyDataAction,
  val controllerComponents: MessagesControllerComponents,
  genericRegistrationService: GrsService,
  businessVerificationLockoutService: BusinessVerificationLockoutService,
  journeyAnswersService: JourneyAnswersService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] =
    (identify andThen getOrCreateJourneyData).async { implicit request =>
      val businessVerification = request.journeyData.businessVerification

      businessVerification.flatMap(_.businessVerificationPassed) match {
        case Some(true) =>
          Future.successful(
            Redirect(routes.TaskListController.onPageLoad())
          )

        case _ =>
          businessVerificationLockoutService.isGroupLockedOut(request.groupId).flatMap {
            case true =>
              logger.warn(
                s"[StartController][onPageLoad] GroupId: ${request.groupId} is locked out of Business Verification journey"
              )
              Future.successful(
                Redirect(routes.BusinessVerificationController.lockout())
              )

            case false =>
              // No company type selection page exists yet - defaults to LimitedCompany
              // until that page is built and starts writing the select company type
              val companyType = businessVerification.flatMap(_.companyType).getOrElse(GrsCompanyType.LimitedCompany)

              persistCompanyType(businessVerification, companyType).flatMap { _ =>
                genericRegistrationService
                  .getGRSJourneyStartUrl(companyType)
                  .map(url => Redirect(url))
                  .recover { case NonFatal(ex) =>
                    logger.error("Failed to fetch GRS journey URL", ex)
                    Redirect(routes.InternalServerErrorController.onPageLoad())
                  }
              }
          }
      }
    }

  private def persistCompanyType(
    existing: Option[BusinessVerification],
    companyType: GrsCompanyType
  )(implicit request: models.requests.DataRequest[?]): Future[BusinessVerification] = {
    val updated = existing
      .map(_.copy(companyType = Some(companyType)))
      .getOrElse(
        BusinessVerification(
          businessRegistrationPassed = None,
          businessVerificationPassed = None,
          utr = None,
          registeredAddress = None,
          companyName = None,
          businessPartnerId = None,
          companyNumber = None,
          companyType = Some(companyType)
        )
      )

    journeyAnswersService.update(updated, request.groupId, request.credentials.providerId)
  }
}
