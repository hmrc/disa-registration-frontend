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

package controllers.liaisonofficers

import controllers.actions.*
import models.journeydata.liaisonofficers.LiaisonOfficer
import models.requests.DataRequest
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.liaisonofficers.{LiaisonOfficerCommunicationSummary, LiaisonOfficerEmailSummary, LiaisonOfficerNameSummary, LiaisonOfficerPhoneNumberSummary}
import viewmodels.govuk.summarylist.*
import views.html.liaisonofficers.LoCheckYourAnswersView

import javax.inject.Inject

class LoCheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: LoCheckYourAnswersView
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  // TODO: Create ticket to ensure CYA validates required journeyData before loading
  def onPageLoad(id: String): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      Ok(view(SummaryListViewModel(buildSummaryRows(id))))
    }

  private def buildSummaryRows(id: String)(implicit request: DataRequest[_], messages: Messages) =
    findLiaisonOfficer(id).toSeq.flatMap { lo =>
      Seq(
        LiaisonOfficerNameSummary.row(lo),
        LiaisonOfficerEmailSummary.row(lo),
        LiaisonOfficerPhoneNumberSummary.row(lo),
        LiaisonOfficerCommunicationSummary.row(lo)
      ).flatten
    }

  private def findLiaisonOfficer(id: String)(implicit request: DataRequest[_]): Option[LiaisonOfficer] =
    request.journeyData.liaisonOfficers.flatMap(_.liaisonOfficers.find(_.id == id))
}
