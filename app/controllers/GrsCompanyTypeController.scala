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
import forms.GrsCompanyTypeFormProvider
import models.grs.GrsCompanyType
import models.journeydata.BusinessVerification
import models.requests.DataRequest
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.GrsCompanyTypeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GrsCompanyTypeController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getOrCreateJourneyData: GetOrCreateJourneyDataAction,
  journeyAnswersService: JourneyAnswersService,
  formProvider: GrsCompanyTypeFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: GrsCompanyTypeView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  def onPageLoad(): Action[AnyContent] =
    (identify andThen getOrCreateJourneyData) { implicit request =>
      val preparedForm = request.journeyData.businessVerification.flatMap(_.companyType).fold(form)(form.fill)
      Ok(view(preparedForm))
    }

  def onSubmit(): Action[AnyContent] =
    (identify andThen getOrCreateJourneyData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
          companyType => persistCompanyType(companyType).map(_ => Redirect(routes.GrsStartController.onPageLoad()))
        )
    }

  private def persistCompanyType(
    companyType: GrsCompanyType
  )(implicit request: DataRequest[?]): Future[BusinessVerification] = {
    val businessVerification = request.journeyData.businessVerification
      .getOrElse(BusinessVerification(None, None, None, None, None, None, None))
      .copy(companyType = Some(companyType))

    journeyAnswersService.update(businessVerification, request.groupId, request.credentials.providerId)
  }
}
