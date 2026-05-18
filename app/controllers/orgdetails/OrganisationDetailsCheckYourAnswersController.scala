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

package controllers.orgdetails

import controllers.actions.*
import models.YesNoAnswer
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.*
import viewmodels.checkAnswers.orgDetails.*
import viewmodels.govuk.summarylist.*
import views.html.orgdetails.OrganisationDetailsCheckYourAnswersView

import javax.inject.Inject

class OrganisationDetailsCheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: OrganisationDetailsCheckYourAnswersView
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>

      val jd         = request.journeyData
      val orgDetails = jd.organisationDetails

      val summaryListRows =
        Seq(
          RegisteredIsaManagerSummary.row(jd),
          ZReferenceNumberSummary.row(jd),
          TradingUsingDifferentNameSummary.row(jd),
          TradingNameSummary.row(jd),
          FirmReferenceNumberSummary.row(jd),
          RegisteredAddressCorrespondenceSummary.row(jd),
          AddedCorrespondenceAddressSummary
            .row(jd)
            .filter(_ => !orgDetails.flatMap(_.registeredAddressCorrespondence).contains(YesNoAnswer.Yes)),
          OrganisationTelephoneNumberSummary.row(jd)
        ).flatten

      Ok(view(SummaryListViewModel(summaryListRows)))
    }
}
