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

package controllers.orgemail

import controllers.actions.*
import controllers.orgemail.routes.{EmailVerificationCodeController, OrganisationEmailAddressController}
import models.NormalMode
import models.journeydata.JourneyData
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.*
import viewmodels.govuk.summarylist.*
import views.html.isaproducts.IsaProductsCheckYourAnswersView

import javax.inject.Inject

class OrganisationEmailCyaController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: IsaProductsCheckYourAnswersView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    verifiedEmailOrRedirect(request.journeyData).fold(
      redirect => redirect,
      email =>
        val summaryListRows = Seq(OrganisationEmailSummary.row(email))
        Ok(view(SummaryListViewModel(summaryListRows)))
    )
  }

  private def verifiedEmailOrRedirect(jd: JourneyData): Either[Result, String] = {
    val section  = jd.organisationEmail
    val email    = section.flatMap(_.organisationEmail)
    val verified = section.flatMap(_.verified)

    email match {
      case None =>
        Left(Redirect(OrganisationEmailAddressController.onPageLoad(NormalMode)))

      case Some(value) if verified.contains(true) =>
        Right(value)

      case Some(_) =>
        Left(Redirect(EmailVerificationCodeController.onPageLoad()))
    }
  }
}
