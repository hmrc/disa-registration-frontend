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

import base.SpecBase
import controllers.routes.TaskListController
import controllers.orgdetails.routes.EnterYourOrganisationAddressController
import forms.EnterYourOrganisationAddressFormProvider
import models.{CheckMode, NormalMode}
import models.journeydata.{CorrespondenceAddress, OrganisationDetails}
import models.journeydata.orgdetails.AddAnotherAddress
import models.journeydata.orgdetails.SelectedCorrespondenceAddress.ManualEntry
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{atMostOnce, never, verify, when}
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.Writes
import play.api.mvc.{Call, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.orgdetails.EnterYourOrganisationAddressView

import scala.concurrent.Future

class EnterYourOrganisationAddressControllerSpec extends SpecBase {

  def onwardRoute: Call = Call("GET", "/foo")

  private val existingAddress =
    CorrespondenceAddress(
      addressLine1 = Some("Old address line 1"),
      addressLine2 = Some("Old address line 2"),
      addressLine3 = Some("Old town"),
      postCode = Some("BB1 1BB")
    )

  private val updatedAddress =
    CorrespondenceAddress(
      addressLine1 = Some("10 Downing Street"),
      addressLine2 = Some("Westminster"),
      addressLine3 = Some("London"),
      postCode = Some("SW1A 2AA")
    )

  private val addAnotherAddress =
    AddAnotherAddress(
      postcode = "SW1A 2AA",
      filter = None,
      addresses = Seq.empty,
      selectedAddress = None
    )

  private val organisationDetails =
    OrganisationDetails(
      correspondenceAddress = Some(existingAddress),
      addAnotherAddress = Some(addAnotherAddress)
    )

  private val journeyDataWithOrganisationDetails =
    emptyJourneyData.copy(
      organisationDetails = Some(organisationDetails)
    )

  lazy val routeUrl: String =
    EnterYourOrganisationAddressController.onPageLoad(NormalMode).url

  lazy val checkRouteUrl: String =
    EnterYourOrganisationAddressController.onPageLoad(CheckMode).url

  lazy val submitUrl: String =
    EnterYourOrganisationAddressController.onSubmit(NormalMode).url

  lazy val checkSubmitUrl: String =
    EnterYourOrganisationAddressController.onSubmit(CheckMode).url

  val formProvider: EnterYourOrganisationAddressFormProvider =
    new EnterYourOrganisationAddressFormProvider()

  val form: Form[CorrespondenceAddress] =
    formProvider()

  "EnterYourOrganisationAddressController" - {

    "must return OK and the correct view for a GET" in {

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EnterYourOrganisationAddressView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val application =
        applicationBuilder(journeyData = Some(journeyDataWithOrganisationDetails)).build()

      running(application) {
        val request = FakeRequest(GET, checkRouteUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EnterYourOrganisationAddressView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(existingAddress), CheckMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must update the section, set selectedAddress to ManualEntry, and redirect to the next page when valid data is submitted" in {

      val expectedSection =
        organisationDetails.copy(
          correspondenceAddress = Some(updatedAddress),
          addAnotherAddress = Some(
            addAnotherAddress.copy(
              selectedAddress = Some(ManualEntry)
            )
          )
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[OrganisationDetails]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyDataWithOrganisationDetails))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(
              "addressLine1" -> "  10 Downing Street  ",
              "addressLine2" -> "  Westminster  ",
              "townOrCity"   -> "  London  ",
              "postcode"     -> "  SW1A 2AA  "
            )

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce())
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[OrganisationDetails]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must update the section and redirect when valid data is submitted and addAnotherAddress does not exist" in {

      val organisationDetailsWithoutAddAnotherAddress =
        OrganisationDetails(
          correspondenceAddress = Some(existingAddress),
          addAnotherAddress = None
        )

      val journeyData =
        emptyJourneyData.copy(
          organisationDetails = Some(organisationDetailsWithoutAddAnotherAddress)
        )

      val expectedSection =
        organisationDetailsWithoutAddAnotherAddress.copy(
          correspondenceAddress = Some(updatedAddress),
          addAnotherAddress = None
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[OrganisationDetails]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(
              "addressLine1" -> "10 Downing Street",
              "addressLine2" -> "Westminster",
              "townOrCity"   -> "London",
              "postcode"     -> "SW1A 2AA"
            )

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce())
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[OrganisationDetails]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return BadRequest and the correct view when invalid data is submitted" in {

      val application =
        applicationBuilder(journeyData = Some(journeyDataWithOrganisationDetails)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(
              "addressLine1" -> "",
              "addressLine2" -> "Westminster",
              "townOrCity"   -> "London",
              "postcode"     -> "SW1A 2AA"
            )

        val boundForm =
          form.bind(
            Map(
              "addressLine1" -> "",
              "addressLine2" -> "Westminster",
              "townOrCity"   -> "London",
              "postcode"     -> "SW1A 2AA"
            )
          )

        val result = route(application, request).value

        val view = application.injector.instanceOf[EnterYourOrganisationAddressView]

        verify(mockJourneyAnswersService, never)
          .update(any[OrganisationDetails], any[String], any[String])(any[Writes[OrganisationDetails]], any)

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the task list when submitted but organisation details do not exist" in {

      val journeyData =
        emptyJourneyData.copy(
          organisationDetails = None
        )

      val application =
        applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(
              "addressLine1" -> "10 Downing Street",
              "addressLine2" -> "Westminster",
              "townOrCity"   -> "London",
              "postcode"     -> "SW1A 2AA"
            )

        val result = route(application, request).value

        verify(mockJourneyAnswersService, never)
          .update(any[OrganisationDetails], any[String], any[String])(any[Writes[OrganisationDetails]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaskListController.onPageLoad().url
      }
    }

    "must return Internal Server Error when JourneyAnswersService.update fails" in {

      val expectedSection =
        organisationDetails.copy(
          correspondenceAddress = Some(updatedAddress),
          addAnotherAddress = Some(
            addAnotherAddress.copy(
              selectedAddress = Some(ManualEntry)
            )
          )
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[OrganisationDetails]], any)
      ).thenReturn(Future.failed(new RuntimeException("fubar")))

      val application =
        applicationBuilder(journeyData = Some(journeyDataWithOrganisationDetails))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(
              "addressLine1" -> "10 Downing Street",
              "addressLine2" -> "Westminster",
              "townOrCity"   -> "London",
              "postcode"     -> "SW1A 2AA"
            )

        await(route(application, request).value)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }
  }
}
