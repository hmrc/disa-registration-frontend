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

package controllers.thirdparty

import controllers.actions.*
import models.journeydata.thirdparty.ThirdParty
import models.requests.DataRequest
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.thirdparty
import viewmodels.checkAnswers.thirdparty.*
import viewmodels.govuk.summarylist.*
import views.html.thirdparty.ThirdPartyCheckYourAnswersView

import javax.inject.Inject

class ThirdPartyCheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: ThirdPartyCheckYourAnswersView
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(id: String): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      findThirdParty(id) match {
        case Some(thirdParty) if !thirdParty.inProgress =>
          Ok(view(SummaryListViewModel(buildSummaryRows(id))))
        case _                                          =>
          Redirect(controllers.routes.TaskListController.onPageLoad())
      }
    }

  private def buildSummaryRows(id: String)(implicit request: DataRequest[_], messages: Messages) =
    request.journeyData.thirdPartyOrganisations.toSeq.flatMap { section =>
      section.thirdParties.zipWithIndex.find { case (tp, _) => tp.id == id }.toSeq.flatMap { case (thirdParty, idx) =>
        val displayIndex = idx + 1
        Seq(
          ThirdPartyOrgDetailsSummary.row(thirdParty, displayIndex),
          ReturnsManagedByThirdPartySummary.row(thirdParty),
          InvestorFundsUsedByThirdPartySummary.row(thirdParty),
          ThirdPartyInvestorFundsPercentageSummary.row(thirdParty)
        ).flatten
      }
    }

  private def findThirdParty(id: String)(implicit request: DataRequest[_]): Option[ThirdParty] =
    request.journeyData.thirdPartyOrganisations.flatMap(_.thirdParties.find(_.id == id))

}
