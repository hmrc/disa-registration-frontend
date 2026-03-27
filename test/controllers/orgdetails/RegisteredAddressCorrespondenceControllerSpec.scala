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
import controllers.*
import forms.RegisteredAddressCorrespondenceFormProvider
import models.{CheckMode, NormalMode}
import models.journeydata.{CorrespondenceAddress, JourneyData, OrganisationDetails, RegisteredAddress}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.libs.json.Writes
import play.api.mvc.{Call, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import views.html.orgdetails.RegisteredAddressCorrespondenceView

import scala.concurrent.Future

class RegisteredAddressCorrespondenceControllerSpec extends SpecBase with MockitoSugar {

  val formProvider        = new RegisteredAddressCorrespondenceFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val registeredAddressCorrespondenceRoute: String =
    orgdetails.routes.RegisteredAddressCorrespondenceController.onPageLoad(NormalMode).url

  "RegisteredAddressCorrespondenceController onPageLoad" - {

    "must return OK and the correct view for a GET when registered address is present" in {

      val journeyData = JourneyData(
        groupId = testGroupId,
        enrolmentId = "enrolment",
        businessVerification = Some(testBV.copy(registeredAddress = Some(testRegisteredAddress)))
      )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, registeredAddressCorrespondenceRoute)
        val view    = application.injector.instanceOf[RegisteredAddressCorrespondenceView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, testRegisteredAddress)(request, messages(application)).toString
      }
    }

    "must redirect to the start page when registered address is missing" in {

      val journeyData = JourneyData(
        groupId = testGroupId,
        enrolmentId = "enrolment",
        businessVerification = Some(testBV.copy(registeredAddress = None))
      )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, registeredAddressCorrespondenceRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.StartController.onPageLoad().url
      }
    }

    "must prepopulate the form when registeredAddressCorrespondence has previously been answered" in {

      val journeyData = JourneyData(
        groupId = testGroupId,
        enrolmentId = "enrolment",
        businessVerification = Some(testBV.copy(registeredAddress = Some(testRegisteredAddress))),
        organisationDetails = Some(OrganisationDetails(registeredAddressCorrespondence = Some(true)))
      )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, registeredAddressCorrespondenceRoute)
        val view    = application.injector.instanceOf[RegisteredAddressCorrespondenceView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode, testRegisteredAddress)(request, messages(application)).toString
      }
    }
  }

  "RegisteredAddressCorrespondenceController onSubmit" - {

    "must update journeyAnswers and populate correspondenceAddress when 'yes' is submitted" in {

      val initialJourneyData = JourneyData(
        groupId = testGroupId,
        enrolmentId = "enrolment",
        businessVerification = Some(testBV.copy(registeredAddress = Some(testRegisteredAddress))),
        organisationDetails = Some(OrganisationDetails())
      )

      val expectedUpdatedSection = OrganisationDetails(
        registeredAddressCorrespondence = Some(true),
        correspondenceAddress = Some(
          CorrespondenceAddress(
            addressLine1 = testRegisteredAddress.addressLine1,
            addressLine2 = testRegisteredAddress.addressLine2,
            addressLine3 = testRegisteredAddress.addressLine3,
            postCode = testRegisteredAddress.postCode
          )
        )
      )

      when(
        mockJourneyAnswersService.update(
          eqTo(expectedUpdatedSection),
          any[String],
          any[String]
        )(any[Writes[OrganisationDetails]], any[HeaderCarrier])
      ).thenReturn(Future.successful(expectedUpdatedSection))

      val application = applicationBuilder(journeyData = Some(initialJourneyData)).build()

      running(application) {
        val request = FakeRequest(POST, registeredAddressCorrespondenceRoute)
          .withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual orgdetails.routes.OrganisationTelephoneNumberController.onPageLoad(NormalMode).url
      }
    }

    "must update journeyAnswers but leave correspondenceAddress empty when 'no' is submitted" in {

      val initialJourneyData = JourneyData(
        groupId = testGroupId,
        enrolmentId = "enrolment",
        businessVerification = Some(testBV.copy(registeredAddress = Some(testRegisteredAddress))),
        organisationDetails = Some(OrganisationDetails())
      )

      val expectedUpdatedSection = OrganisationDetails(
        registeredAddressCorrespondence = Some(false),
        correspondenceAddress = None
      )

      when(mockJourneyAnswersService.update(eqTo(expectedUpdatedSection), any[String], any[String])
        (any[Writes[OrganisationDetails]], any)).thenReturn(Future.successful(expectedUpdatedSection))

      val application = applicationBuilder(journeyData = Some(initialJourneyData)).build()

      running(application) {
        val request = FakeRequest(POST, registeredAddressCorrespondenceRoute)
          .withFormUrlEncodedBody("value" -> "false")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.IndexController.onPageLoad().url
      }
    }

    "must return to check your answers if stays no answer has changed on submission" in {

      val initialJourneyData = JourneyData(
        groupId = testGroupId,
        enrolmentId = "enrolment",
        businessVerification = Some(testBV.copy(registeredAddress = Some(testRegisteredAddress))),
        organisationDetails = Some(OrganisationDetails(registeredAddressCorrespondence = Some(false)))
      )

      val expectedUpdatedSection = OrganisationDetails(
        registeredAddressCorrespondence = Some(false),
        correspondenceAddress = None
      )

      when(mockJourneyAnswersService.update(eqTo(expectedUpdatedSection), any[String], any[String])
        (any[Writes[OrganisationDetails]], any)).thenReturn(Future.successful(expectedUpdatedSection))

      val application = applicationBuilder(journeyData = Some(initialJourneyData)).build()

      running(application) {
        val request = FakeRequest(POST, orgdetails.routes.RegisteredAddressCorrespondenceController.onPageLoad(CheckMode).url)
          .withFormUrlEncodedBody("value" -> "false")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.IndexController.onPageLoad().url
      }
    }

    "must return BAD_REQUEST when invalid data is submitted" in {

      val journeyData = JourneyData(
        groupId = testGroupId,
        enrolmentId = "enrolment",
        businessVerification = Some(testBV.copy(registeredAddress = Some(testRegisteredAddress)))
      )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(POST, registeredAddressCorrespondenceRoute)
          .withFormUrlEncodedBody("value" -> "")

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[RegisteredAddressCorrespondenceView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, testRegisteredAddress)(request, messages(application)).toString
      }
    }

    "must redirect to start page when registered address is missing on submit" in {

      val initialJourneyData = JourneyData(
        groupId = testGroupId,
        enrolmentId = "enrolment",
        businessVerification = Some(testBV.copy(registeredAddress = None))
      )

      val application = applicationBuilder(journeyData = Some(initialJourneyData)).build()

      running(application) {
        val request = FakeRequest(POST, registeredAddressCorrespondenceRoute)
          .withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.StartController.onPageLoad().url
      }
    }

    "must return InternalServerError when updateJourneyAnswers fails" in {

      val initialJourneyData = JourneyData(
        groupId = testGroupId,
        enrolmentId = "enrolment",
        businessVerification = Some(testBV.copy(registeredAddress = Some(testRegisteredAddress)))
      )

      when(mockJourneyAnswersService.update(any[OrganisationDetails], any[String], any[String])
        (any[Writes[OrganisationDetails]], any)).thenReturn(Future.failed(new Exception))

      val application = applicationBuilder(journeyData = Some(initialJourneyData))
        .build()

      running(application) {
        val request = FakeRequest(POST, registeredAddressCorrespondenceRoute)
          .withFormUrlEncodedBody("value" -> "true")

        await(route(application, request).value)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }
  }
}