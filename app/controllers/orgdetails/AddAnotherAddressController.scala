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

import config.FrontendAppConfig
import controllers.actions.*
import controllers.routes.*
import forms.{AddAnotherAddressFormProvider, ThirdPartyOrgDetailsFormProvider}
import handlers.ErrorHandler
import models.Mode
import models.journeydata.orgdetails.AddAnotherAddressForm
import models.journeydata.thirdparty.*
import models.requests.DataRequest
import navigation.Navigator
import pages.organisationdetails.AddAnotherAddressPage
import pages.thirdparty.ThirdPartyOrgDetailsPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.UuidGenerator
import views.html.orgdetails.AddAnotherAddressView
import views.html.thirdparty.ThirdPartyOrgDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class AddAnotherAddressController @Inject()(
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  journeyAnswersService: JourneyAnswersService,
  errorHandler: ErrorHandler,
  formProvider: AddAnotherAddressFormProvider,
  navigator: Navigator,
  uuidGenerator: UuidGenerator,
  val controllerComponents: MessagesControllerComponents,
  view: AddAnotherAddressView,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[AddAnotherAddressForm] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      val preparedForm =
        request.journeyData.organisationDetails
          .flatMap(_.addAnotherAddress)
          .map(form.fill)
          .getOrElse(form)
      Ok(view(preparedForm, mode))
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, mode))),

          answer => {

            val updatedOrgDetails =
              request.journeyData.organisationDetails
                .map { existing =>
                  existing.copy(
                    addAnotherAddress = Some(answer)
                  )
                }

            updatedOrgDetails match {

              case Some(updatedSection) =>
                journeyAnswersService
                  .update(
                    updatedSection,
                    request.groupId,
                    request.credentials.providerId
                  )
                  .map { updatedJourneyData =>
                    Redirect(
                      navigator.nextPage(
                        AddAnotherAddressPage,
                        updatedJourneyData,
                        mode
                      )
                    )
                  }
                  .recoverWith { case NonFatal(e) =>
                    logger.warn(
                      s"Failed updating answers for OrganisationDetails addAnotherAddress " +
                        s"[groupId=${request.groupId}] error: ${e.getMessage}"
                    )
                    errorHandler.internalServerError
                  }

              case None =>
                Future.successful(
                  Redirect(TaskListController.onPageLoad())
                )
            }
          }
        )
    }
}
