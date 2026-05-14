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
import forms.AddAnotherAddressFormProvider
import handlers.ErrorHandler
import models.{Mode, ReturnTo}
import models.addresslookup.LookupAddress
import models.journeydata.{CorrespondenceAddress, OrganisationDetails}
import models.journeydata.orgdetails.{AddAnotherAddress, SelectedCorrespondenceAddress}
import navigation.Navigator
import pages.organisationdetails.AddAnotherAddressPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{AddressLookupService, JourneyAnswersService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.orgdetails.AddAnotherAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class AddAnotherAddressController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  journeyAnswersService: JourneyAnswersService,
  addressLookupService: AddressLookupService,
  errorHandler: ErrorHandler,
  formProvider: AddAnotherAddressFormProvider,
  navigator: Navigator,
  val controllerComponents: MessagesControllerComponents,
  view: AddAnotherAddressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val form: Form[AddAnotherAddress] = formProvider()

  def onPageLoad(mode: Mode, returnTo: Option[ReturnTo]): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      val preparedForm =
        request.journeyData.organisationDetails
          .flatMap(_.addAnotherAddress)
          .map(form.fill)
          .getOrElse(form)

      Ok(view(preparedForm, mode, returnTo))
    }

  def onSubmit(mode: Mode, returnTo: Option[ReturnTo]): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, returnTo))),
          answer =>
            addressLookupService.lookup(answer.postcode, answer.filter).flatMap { results =>
              val enrichedAnswer = answer.copy(addresses = results)
              val updatedSection =
                buildUpdatedSection(request.journeyData.organisationDetails, enrichedAnswer, results)

              journeyAnswersService
                .update(
                  updatedSection,
                  request.groupId,
                  request.credentials.providerId
                )
                .map { updatedSection =>
                  if (results.isEmpty) {
                    val formWithError =
                      form
                        .fill(enrichedAnswer)
                        .withError(
                          "postcode",
                          "AddAnotherAddress.postcode.error.not.found"
                        )
                    BadRequest(view(formWithError, mode, returnTo))
                  } else {
                    Redirect(navigator.nextPage(AddAnotherAddressPage, updatedSection, mode, returnTo))
                  }
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

  private def buildUpdatedSection(
    existingDetails: Option[OrganisationDetails],
    enrichedAnswer: AddAnotherAddress,
    results: Seq[LookupAddress]
  ): OrganisationDetails =
    results match {

      case Seq(singleAddress) =>
        val updatedAnswer =
          enrichedAnswer.copy(selectedAddress = Some(SelectedCorrespondenceAddress.Address(0)))

        existingDetails match {
          case Some(existing) =>
            existing.copy(
              correspondenceAddress = Some(CorrespondenceAddress.fromLookup(singleAddress)),
              addAnotherAddress = Some(updatedAnswer)
            )

          case None =>
            OrganisationDetails(
              correspondenceAddress = Some(CorrespondenceAddress.fromLookup(singleAddress)),
              addAnotherAddress = Some(updatedAnswer)
            )
        }

      case _ =>
        existingDetails match {
          case Some(existing) =>
            existing.copy(addAnotherAddress = Some(enrichedAnswer))
          case None           =>
            OrganisationDetails(addAnotherAddress = Some(enrichedAnswer))
        }
    }
}
