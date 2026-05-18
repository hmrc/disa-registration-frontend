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
import forms.AddAnotherAddressFormProvider
import models.{Mode, NormalMode}
import models.journeydata.certificatesofauthority.CertificatesOfAuthority
import models.journeydata.orgdetails.AddAnotherAddress
import models.journeydata.{JourneyData, OrganisationDetails}
import navigation.Navigator
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{atMostOnce, verify, when}
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.Writes
import play.api.mvc.Results.InternalServerError
import play.api.mvc.{Call, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.orgdetails.AddAnotherAddressView

import scala.concurrent.Future

class AddAnotherAddressControllerSpec extends SpecBase {

  private val formProvider                  = new AddAnotherAddressFormProvider()
  private val form: Form[AddAnotherAddress] = formProvider()

  private val mode: Mode = NormalMode

  private val baseAnswer =
    AddAnotherAddress(
      postcode = "AA1 1AA",
      filter = Some("Test"),
      addresses = Seq.empty,
      selectedAddress = None
    )

  private val journeyDetails =
    OrganisationDetails(addAnotherAddress = None)

  private def onwardRoute =
    Call("GET", "/next-page")

  "AddAnotherAddressController onPageLoad" - {

    "must return OK with empty form when no data exists" in {

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData.copy(organisationDetails = Some(journeyDetails))))
          .build()

      running(application) {
        val request =
          FakeRequest(GET, routes.AddAnotherAddressController.onPageLoad(mode, None).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddAnotherAddressView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, mode, None)(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate form when existing answer is present" in {

      val filledJourney =
        emptyJourneyData.copy(
          organisationDetails = Some(
            journeyDetails.copy(addAnotherAddress = Some(baseAnswer))
          )
        )

      val application =
        applicationBuilder(journeyData = Some(filledJourney)).build()

      running(application) {
        val request =
          FakeRequest(GET, routes.AddAnotherAddressController.onPageLoad(mode, None).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddAnotherAddressView]

        val expectedForm = form.fill(baseAnswer)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(expectedForm, mode, None)(
          request,
          messages(application)
        ).toString
      }
    }
  }

  "AddAnotherAddressController onSubmit" - {

    "must return BAD_REQUEST when form is invalid" in {

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData.copy(organisationDetails = Some(journeyDetails))))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.AddAnotherAddressController.onSubmit(mode, None).url)
            .withFormUrlEncodedBody(
              "postcode" -> "",
              "filter"   -> "x"
            )

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include("error")
      }
    }

    "must redirect when lookup succeeds and results are returned" in {

      when(mockAddressLookupService.lookup(any[String], any())(any()))
        .thenReturn(Future.successful(Seq(mock[models.addresslookup.LookupAddress])))

      when(
        mockJourneyAnswersService.update(any(), any[String], any[String])(
          any[Writes[OrganisationDetails]],
          any
        )
      ).thenReturn(Future.successful(journeyDetails))

      val navigator = mock[Navigator]
      when(navigator.nextPage(any(), any(), any(), any())).thenReturn(onwardRoute)

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData.copy(organisationDetails = Some(journeyDetails))))
          .overrides(bind[Navigator].toInstance(navigator))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.AddAnotherAddressController.onSubmit(mode, None).url)
            .withFormUrlEncodedBody(
              "postcode" -> "AA1 1AA",
              "filter"   -> "Test"
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        verify(mockJourneyAnswersService, atMostOnce)
          .update(any(), any[String], any[String])(any(), any)
      }
    }

    "must return BAD_REQUEST when lookup returns empty results" in {

      when(mockAddressLookupService.lookup(any[String], any())(any()))
        .thenReturn(Future.successful(Seq.empty))

      when(mockJourneyAnswersService.update(any(), any[String], any[String])(any(), any))
        .thenReturn(Future.successful(journeyDetails))

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData.copy(organisationDetails = Some(journeyDetails))))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.AddAnotherAddressController.onSubmit(mode, None).url)
            .withFormUrlEncodedBody(
              "postcode" -> "AA1 1AA",
              "filter"   -> "Test"
            )

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must return 500 when journey update fails" in {

      when(mockAddressLookupService.lookup(any[String], any())(any()))
        .thenReturn(Future.successful(Seq(mock[models.addresslookup.LookupAddress])))

      when(mockJourneyAnswersService.update(any(), any[String], any[String])(any[Writes[OrganisationDetails]], any))
        .thenReturn(Future.failed(new Exception("DB failure")))

      when(mockErrorHandler.internalServerError(any[RequestHeader]))
        .thenReturn(Future.successful(InternalServerError))

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData.copy(organisationDetails = Some(journeyDetails))))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.AddAnotherAddressController.onSubmit(mode, None).url)
            .withFormUrlEncodedBody(
              "postcode" -> "AA1 1AA",
              "filter"   -> "Test"
            )

        val result = route(application, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
      }
    }

    "must create OrganisationDetails when none exists in journey data" in {

      val emptyJourney =
        emptyJourneyData.copy(
          organisationDetails = None
        )

      val enriched =
        baseAnswer.copy(
          addresses = Seq(mock[models.addresslookup.LookupAddress])
        )

      val expectedSection =
        OrganisationDetails(
          addAnotherAddress = Some(enriched)
        )

      when(mockAddressLookupService.lookup(any[String], any())(any()))
        .thenReturn(Future.successful(Seq(mock[models.addresslookup.LookupAddress])))

      when(
        mockJourneyAnswersService.update(
          any(),
          any[String],
          any[String]
        )(any[Writes[OrganisationDetails]], any())
      ).thenReturn(Future.successful(expectedSection))

      val navigator = mock[Navigator]
      when(navigator.nextPage(any(), any(), any(), any()))
        .thenReturn(Call("GET", "/next-page"))

      val application =
        applicationBuilder(journeyData = Some(emptyJourney))
          .overrides(bind[Navigator].toInstance(navigator))
          .build()

      running(application) {

        val request =
          FakeRequest(POST, routes.AddAnotherAddressController.onSubmit(mode, None).url)
            .withFormUrlEncodedBody(
              "postcode" -> "AA1 1AA",
              "filter"   -> "Test"
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any(), any)
      }
    }
  }

  "AddAnotherAddressController clearCorrespondenceAddressAndRedirect" - {

    "must clear correspondenceAddress and addAnotherAddress and redirect" in {

      val existingDetails =
        OrganisationDetails(
          correspondenceAddress = Some(mock[models.journeydata.CorrespondenceAddress]),
          addAnotherAddress = Some(baseAnswer)
        )

      val updatedSection =
        existingDetails.copy(
          correspondenceAddress = None,
          addAnotherAddress = None
        )

      val journeyData =
        emptyJourneyData.copy(
          organisationDetails = Some(existingDetails)
        )

      when(
        mockJourneyAnswersService.update(
          eqTo(updatedSection),
          any[String],
          any[String]
        )(any[Writes[OrganisationDetails]], any())
      ).thenReturn(Future.successful(updatedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {

        val request =
          FakeRequest(
            GET,
            routes.AddAnotherAddressController
              .clearCorrespondenceAddressAndRedirect()
              .url
          )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.orgdetails.routes.EnterYourOrganisationAddressController
            .onPageLoad(NormalMode, None)
            .url

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(updatedSection), any[String], any[String])(any(), any)
      }
    }

    "must create empty OrganisationDetails when none exists" in {

      when(
        mockJourneyAnswersService.update(
          eqTo(OrganisationDetails()),
          any[String],
          any[String]
        )(any[Writes[OrganisationDetails]], any())
      ).thenReturn(Future.successful(OrganisationDetails()))

      val application =
        applicationBuilder(
          journeyData = Some(
            emptyJourneyData.copy(organisationDetails = None)
          )
        ).build()

      running(application) {

        val request =
          FakeRequest(
            GET,
            routes.AddAnotherAddressController
              .clearCorrespondenceAddressAndRedirect()
              .url
          )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.orgdetails.routes.EnterYourOrganisationAddressController
            .onPageLoad(NormalMode, None)
            .url
      }
    }

    "must return INTERNAL_SERVER_ERROR when update fails" in {

      when(
        mockJourneyAnswersService.update(
          any(),
          any[String],
          any[String]
        )(any[Writes[OrganisationDetails]], any())
      ).thenReturn(Future.failed(new Exception("DB failure")))

      when(mockErrorHandler.internalServerError(any[RequestHeader]))
        .thenReturn(Future.successful(InternalServerError))

      val application =
        applicationBuilder(
          journeyData = Some(
            emptyJourneyData.copy(
              organisationDetails = Some(journeyDetails)
            )
          )
        ).build()

      running(application) {

        val request =
          FakeRequest(
            GET,
            routes.AddAnotherAddressController
              .clearCorrespondenceAddressAndRedirect()
              .url
          )

        val result = route(application, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
      }
    }
  }
}
