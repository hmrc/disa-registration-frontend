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
import controllers.orgdetails.routes.*
import controllers.routes.*
import forms.ChooseAddressFormProvider
import models.NormalMode
import models.addresslookup.LookupAddress
import models.journeydata.orgdetails.{AddAnotherAddress, ChooseAddressAnswer}
import models.journeydata.{CorrespondenceAddress, JourneyData, OrganisationDetails}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.libs.json.Writes
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.orgdetails.ChooseAddressView

import scala.concurrent.Future

class ChooseAddressControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = TaskListController.onPageLoad()

  val formProvider       = new ChooseAddressFormProvider()
  val form: Form[String] = formProvider()

  private val address1 =
    LookupAddress(
      addressLine1 = Some("1 Test Street"),
      addressLine2 = Some("London"),
      addressLine3 = None,
      postCode = Some(testString)
    )

  private val address2 =
    LookupAddress(
      addressLine1 = Some("2 Test Street"),
      addressLine2 = Some("London"),
      addressLine3 = None,
      postCode = Some(testString)
    )

  private val addresses = Seq(address1, address2)

  val journeyData: JourneyData =
    JourneyData(
      groupId = testGroupId,
      enrolmentId = testString,
      organisationDetails = Some(
        OrganisationDetails(
          addAnotherAddress = Some(
            AddAnotherAddress(postcode = testString, filter = None, addresses = addresses)
          ),
          chooseAddressAnswer = None
        )
      )
    )

  lazy val routeUrl = ChooseAddressController.onPageLoad(NormalMode).url

  lazy val submitUrl = ChooseAddressController.onSubmit(NormalMode).url

  "ChooseAddressController" - {

    "must return OK and load page with empty form" in {

      val application =
        applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {

        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ChooseAddressView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, addresses, NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must prepopulate form when previously answered (None selected)" in {

      val updatedJourney =
        journeyData.copy(
          organisationDetails = journeyData.organisationDetails.map(
            _.copy(
              chooseAddressAnswer = Some(ChooseAddressAnswer.NoneOfThese)
            )
          )
        )

      val application =
        applicationBuilder(journeyData = Some(updatedJourney)).build()

      running(application) {

        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("value=\"none\"")
      }
    }

    "must redirect when valid address index submitted" in {

      val expected =
        OrganisationDetails(
          addAnotherAddress = Some(AddAnotherAddress(postcode = testString, filter = None, addresses = addresses)),
          chooseAddressAnswer = Some(
            ChooseAddressAnswer.Selected(
              CorrespondenceAddress.fromLookup(address1)
            )
          )
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expected), any[String], any[String])(any[Writes[OrganisationDetails]], any)
      ).thenReturn(Future.successful(expected))

      val application =
        applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {

        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> "0")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
      }
    }

    "must redirect when None of these selected" in {

      val expected =
        OrganisationDetails(
          addAnotherAddress = Some(AddAnotherAddress(postcode = testString, filter = None, addresses = addresses)),
          chooseAddressAnswer = Some(ChooseAddressAnswer.NoneOfThese)
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expected), any[String], any[String])(any[Writes[OrganisationDetails]], any)
      ).thenReturn(Future.successful(expected))

      val application =
        applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {

        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> "none")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
      }
    }

    "must return BAD_REQUEST when form invalid" in {

      val application =
        applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {

        val request = FakeRequest(POST, submitUrl)

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must return INTERNAL_SERVER_ERROR when service fails" in {

      when(
        mockJourneyAnswersService
          .update(any[OrganisationDetails], any[String], any[String])(any[Writes[OrganisationDetails]], any)
      ).thenReturn(Future.failed(new Exception("boom")))

      val application =
        applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {

        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> "0")

        val result = route(application, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR

        verify(mockErrorHandler).internalServerError(any)
      }
    }
  }
}
