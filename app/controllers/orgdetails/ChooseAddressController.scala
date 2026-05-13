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

import config.Constants.noneRadioValue
import controllers.actions.*
import forms.ChooseAddressFormProvider
import handlers.ErrorHandler
import models.Mode
import models.addresslookup.LookupAddress
import models.journeydata.{CorrespondenceAddress, OrganisationDetails}
import models.journeydata.orgdetails.SelectedCorrespondenceAddress
import models.journeydata.orgdetails.SelectedCorrespondenceAddress.*
import navigation.Navigator
import pages.organisationdetails.ChooseAddressPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.orgdetails.ChooseAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class ChooseAddressController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  journeyAnswersService: JourneyAnswersService,
  errorHandler: ErrorHandler,
  formProvider: ChooseAddressFormProvider,
  navigator: Navigator,
  val controllerComponents: MessagesControllerComponents,
  view: ChooseAddressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>

      val organisationDetails = request.journeyData.organisationDetails
      val addresses           = extractAddresses(organisationDetails)

      val preparedForm =
        preselectedValue(organisationDetails)
          .fold(form)(form.fill)

      Ok(view(preparedForm, addresses, mode))
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val organisationDetails = request.journeyData.organisationDetails
      val addresses           = extractAddresses(organisationDetails)
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors, addresses, mode))
            ),
          answer => {
            val selectedAddress =
              SelectedCorrespondenceAddress.fromFormValue(answer, addresses)

            val correspondenceAddress =
              CorrespondenceAddress.fromSelectedAddress(selectedAddress, addresses)

            val updatedAddAnotherAddress =
              organisationDetails
                .flatMap(_.addAnotherAddress)
                .map(_.copy(selectedAddress = Some(selectedAddress)))

            val updatedSection =
              organisationDetails match {
                case Some(existing) =>
                  existing
                    .copy(correspondenceAddress = correspondenceAddress, addAnotherAddress = updatedAddAnotherAddress)
                case None           =>
                  OrganisationDetails(
                    correspondenceAddress = correspondenceAddress,
                    addAnotherAddress = updatedAddAnotherAddress
                  )
              }

            journeyAnswersService
              .update(
                updatedSection,
                request.groupId,
                request.credentials.providerId
              )
              .map { persistedSection =>
                Redirect(navigator.nextPage(ChooseAddressPage, persistedSection, mode, None))
              }
              .recoverWith { case NonFatal(e) =>
                logger.warn(
                  s"Failed updating answers for section [${updatedSection.sectionName}] " +
                    s"for groupId [${request.groupId}] with error: [$e]"
                )
                errorHandler.internalServerError
              }
          }
        )
    }

  private def preselectedValue(
    organisationDetails: Option[OrganisationDetails]
  ): Option[String] =
    organisationDetails
      .flatMap(_.addAnotherAddress)
      .flatMap(_.selectedAddress)
      .map {
        case SelectedCorrespondenceAddress.Address(index) =>
          index.toString
        case ManualEntry                                  =>
          noneRadioValue
      }

  private def extractAddresses(
    organisationDetails: Option[OrganisationDetails]
  ): Seq[LookupAddress] =
    organisationDetails
      .flatMap(_.addAnotherAddress)
      .map(_.addresses)
      .getOrElse(Seq.empty)
}
