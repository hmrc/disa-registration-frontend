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

package controllers.isaproducts

import controllers.actions.*
import models.journeydata.JourneyData
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, RequestHeader}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.*
import viewmodels.checkAnswers.isaproducts.{InnovativeFinancialProductsSummary, IsaProductsSummary, PeerToPeerPlatformNumberSummary, PeerToPeerPlatformSummary}
import viewmodels.govuk.summarylist.*
import views.html.isaproducts.IsaProductsCheckYourAnswersView

import javax.inject.Inject

class IsaProductsCheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  grsGuard: GrsGuardActionFilter,
  cyaGuard: CyaGuardAction,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  val controllerComponents: MessagesControllerComponents,
  view: IsaProductsCheckYourAnswersView
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen grsGuard) { implicit request =>
    cyaGuard.sortingData(request.journeyData.get.isaProducts) match {
      case Some(call) => Redirect(call)
      case _          => Ok(view(SummaryListViewModel(summaryListRows(request.journeyData))))
    }
  }

  private def summaryListRows(journeyData: Option[JourneyData])(implicit request: RequestHeader): Seq[SummaryListRow] =
    journeyData.toSeq.flatMap { jd =>
      Seq(
        IsaProductsSummary.row(jd),
        InnovativeFinancialProductsSummary.row(jd),
        PeerToPeerPlatformSummary.row(jd),
        PeerToPeerPlatformNumberSummary.row(jd)
      )
    }.flatten

}
