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

package controllers.orgdetails

import controllers.actions.*
import forms.RegisteredAddressCorrespondenceFormProvider
import handlers.ErrorHandler
import models.Mode
import models.journeydata.{CorrespondenceAddress, OrganisationDetails, RegisteredAddress}
import navigation.Navigator
import pages.organisationdetails.RegisteredAddressCorrespondencePage
import play.api.data.Form
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.orgdetails.RegisteredAddressCorrespondenceView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegisteredAddressCorrespondenceController @Inject()(
                                                           override val messagesApi: MessagesApi,
                                                           navigator: Navigator,
                                                           identify: IdentifierAction,
                                                           getData: DataRetrievalAction,
                                                           formProvider: RegisteredAddressCorrespondenceFormProvider,
                                                           journeyAnswersService: JourneyAnswersService,
                                                           errorHandler: ErrorHandler,
                                                           val controllerComponents: MessagesControllerComponents,
                                                           view: RegisteredAddressCorrespondenceView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData) { implicit request =>

    (for {
      jd <- request.journeyData
      bv <- jd.businessVerification
      regAddr <- bv.registeredAddress
    } yield regAddr) match {

      case None =>
        Redirect(controllers.routes.StartController.onPageLoad())

      case Some(registeredAddress) =>
        val preparedForm = request.journeyData
          .flatMap(_.organisationDetails)
          .flatMap(_.registeredAddressCorrespondence)
          .fold(form)(form.fill)

        Ok(view(preparedForm, mode, registeredAddress))
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData).async { implicit request =>

    val registeredAddress =
      for {
        jd <- request.journeyData
        bv <- jd.businessVerification
        addr <- bv.registeredAddress
      } yield addr

    registeredAddress match {
      case None =>
        Future.successful(Redirect(controllers.routes.StartController.onPageLoad()))
      case Some(registeredAddress) =>
        form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, mode, registeredAddress))),
          answer => {
            val updatedOrganisationDetails =
              buildOrganisationDetails(request.journeyData.flatMap(_.organisationDetails), answer, registeredAddress)

            journeyAnswersService
              .update(updatedOrganisationDetails, request.groupId, request.credentials.providerId)
              .map { updated =>
                Redirect(navigator.nextPage(RegisteredAddressCorrespondencePage, updated, mode))
              }
              .recoverWith { case e =>
                logger.warn(
                  s"Failed updating answers for section [${updatedOrganisationDetails.sectionName}] for groupId [${request.groupId}] with error: [$e]"
                )
                errorHandler.internalServerError
              }
          }
        )
    }
  }

  private def buildOrganisationDetails(
                                        existing: Option[OrganisationDetails],
                                        answer: Boolean,
                                        registeredAddress: RegisteredAddress
                                      ): OrganisationDetails = {

    val correspondenceAddress =
      CorrespondenceAddress(
        addressLine1 = registeredAddress.addressLine1,
        addressLine2 = registeredAddress.addressLine2,
        addressLine3 = registeredAddress.addressLine3,
        postCode = registeredAddress.postCode
      )

    val base = existing.getOrElse(OrganisationDetails())

    base.copy(
      registeredAddressCorrespondence = Some(answer),
      correspondenceAddress =
        if (answer) Some(correspondenceAddress) else None
    )
  }
}
