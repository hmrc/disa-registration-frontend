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

package controllers.certificatesofauthority

import controllers.actions.*
import models.journeydata.JourneyData
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.*
import viewmodels.govuk.summarylist.*
import views.html.certificatesofauthority.CoaCheckYourAnswersView
import views.html.isaproducts.IsaProductsCheckYourAnswersView

import javax.inject.Inject

class CoaCheckYourAnswersController @Inject()(
                                               override val messagesApi: MessagesApi,
                                               identify: IdentifierAction,
                                               getData: DataRetrievalAction,
                                               requireData: DataRequiredAction,
                                               val controllerComponents: MessagesControllerComponents,
                                               view: CoaCheckYourAnswersView
                                             ) extends FrontendBaseController
  with I18nSupport
  with Logging {

  // TODO: Create ticket to ensure CYA validates required journeyData before loading
  def onPageLoad(): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      Ok(view(SummaryListViewModel(buildSummaryRows(request.journeyData))))
    }

  private def buildSummaryRows(journeyData: JourneyData)(implicit messages: Messages) =
    Seq(
      CertificatesOfAuthorityYesNoSummary.row(journeyData),
      FcaArticlesSummary.row(journeyData),
      FinancialOrganisationSummary.row(journeyData)
    ).flatten

}
