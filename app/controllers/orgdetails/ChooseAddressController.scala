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
import forms.ChooseAddressFormProvider
import handlers.ErrorHandler
import models.Mode
import models.addresslookup.LookupAddress
import models.journeydata.{CorrespondenceAddress, OrganisationDetails}
import models.journeydata.orgdetails.ChooseAddressAnswer
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
        preselectedValue(organisationDetails, addresses)
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
            val chooseAddressAnswer: ChooseAddressAnswer =
              parseAnswer(answer, addresses)
            val updatedSection: OrganisationDetails      =
              organisationDetails match {
                case Some(existing) =>
                  existing.copy(chooseAddressAnswer = Some(chooseAddressAnswer))
                case None           =>
                  OrganisationDetails(
                    chooseAddressAnswer = Some(chooseAddressAnswer)
                  )
              }

            journeyAnswersService
              .update(
                updatedSection,
                request.groupId,
                request.credentials.providerId
              )
              .map { persistedSection =>
                Redirect(
                  navigator.nextPage(
                    ChooseAddressPage,
                    persistedSection,
                    mode,
                    None
                  )
                )
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

  private def parseAnswer(
    answer: String,
    addresses: Seq[LookupAddress]
  ): ChooseAddressAnswer =
    answer match {
      case "none" =>
        ChooseAddressAnswer.NoneOfThese

      case idx =>
        val address =
          addresses
            .lift(idx.toInt)
            .getOrElse(
              throw new IllegalArgumentException("Invalid address index")
            )

        ChooseAddressAnswer.Selected(
          CorrespondenceAddress.fromLookup(address)
        )
    }

  private def preselectedValue(
    organisationDetails: Option[OrganisationDetails],
    addresses: Seq[LookupAddress]
  ): Option[String] =
    organisationDetails.flatMap(_.chooseAddressAnswer).map {

      case ChooseAddressAnswer.NoneOfThese =>
        "none"

      case ChooseAddressAnswer.Selected(selected) =>
        addresses.zipWithIndex
          .find { case (lookup, _) =>
            CorrespondenceAddress.matches(selected, lookup)
          }
          .map(_._2.toString)
          .getOrElse("none")
    }

  private def extractAddresses(
    organisationDetails: Option[OrganisationDetails]
  ): Seq[LookupAddress] =
    organisationDetails
      .flatMap(_.addAnotherAddress)
      .map(_.addresses)
      .getOrElse(Seq.empty)
}
