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

package controllers.thirdparty

import base.SpecBase
import controllers.thirdparty.routes.ProductsManagedByThirdPartyController
import forms.YesNoAnswerFormProvider
import models.YesNoAnswer
import models.YesNoAnswer.*
import models.journeydata.JourneyData
import models.journeydata.thirdparty.{ThirdParty, ThirdPartyOrganisations}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{atMostOnce, verify, when}
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.Writes
import play.api.mvc.{Call, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.thirdparty.ProductsManagedByThirdPartyView

import scala.concurrent.Future

class ProductsManagedByThirdPartyControllerSpec extends SpecBase {

  def onwardRoute: Call = Call("GET", "/foo")

  lazy val routeUrl: String  = ProductsManagedByThirdPartyController.onPageLoad().url
  lazy val submitUrl: String = ProductsManagedByThirdPartyController.onSubmit().url

  val formProvider: YesNoAnswerFormProvider = new YesNoAnswerFormProvider()
  val form: Form[YesNoAnswer]               = formProvider("productsManagedByThirdParty.error.required")

  "ProductsManagedByThirdPartyController" - {

    "must return OK and the correct view for a GET when there is no existing answer" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ProductsManagedByThirdPartyView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when previously answered yes" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          thirdPartyOrganisations = Some(
            ThirdPartyOrganisations(
              managedByThirdParty = Some(Yes)
            )
          )
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ProductsManagedByThirdPartyView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(Yes))(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when previously answered no" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          thirdPartyOrganisations = Some(
            ThirdPartyOrganisations(
              managedByThirdParty = Some(No)
            )
          )
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ProductsManagedByThirdPartyView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(No))(request, messages(application)).toString
      }
    }

    "must return BadRequest and errors when invalid data is submitted" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> "")

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[ProductsManagedByThirdPartyView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted and the section is created" in {

      val expectedSection =
        ThirdPartyOrganisations(
          managedByThirdParty = Some(Yes)
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[ThirdPartyOrganisations]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> "yes")

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[ThirdPartyOrganisations]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the next page when valid data is submitted and the existing section is updated" in {

      val existingSection =
        ThirdPartyOrganisations(
          managedByThirdParty = Some(No)
        )

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          thirdPartyOrganisations = Some(existingSection)
        )

      val expectedSection =
        ThirdPartyOrganisations(
          managedByThirdParty = Some(Yes)
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[ThirdPartyOrganisations]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> "yes")

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[ThirdPartyOrganisations]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must clear stale pages when valid data is submitted and the existing section is updated" in {

      val existingSection =
        ThirdPartyOrganisations(
          managedByThirdParty = Some(Yes),
          thirdParties = Seq(ThirdParty(testString))
        )

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          thirdPartyOrganisations = Some(existingSection)
        )

      val expectedSection =
        ThirdPartyOrganisations(
          managedByThirdParty = Some(No)
        )

      when(
        mockJourneyAnswersService
          .update(any[ThirdPartyOrganisations], any[String], any[String])(any[Writes[ThirdPartyOrganisations]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> "no")

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[ThirdPartyOrganisations]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return Internal Server Error when JourneyAnswersService.update fails" in {

      val expectedSection =
        ThirdPartyOrganisations(
          managedByThirdParty = Some(Yes)
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[ThirdPartyOrganisations]], any)
      ).thenReturn(Future.failed(new Exception("fubar")))

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> "yes")

        await(route(application, request).value)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }

    "must call navigator.nextPage with the correct page and sections" in {

      val existingSection =
        ThirdPartyOrganisations(
          managedByThirdParty = Some(No)
        )

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          thirdPartyOrganisations = Some(existingSection)
        )

      val updatedSection =
        ThirdPartyOrganisations(
          managedByThirdParty = Some(Yes)
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(updatedSection), any[String], any[String])(any[Writes[ThirdPartyOrganisations]], any)
      ).thenReturn(Future.successful(updatedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> "yes")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }
  }
}
