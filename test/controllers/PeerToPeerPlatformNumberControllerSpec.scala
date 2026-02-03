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

package controllers

import base.SpecBase
import controllers.isaproducts.routes.PeerToPeerPlatformNumberController
import forms.PeerToPeerPlatformNumberFormProvider
import handlers.ErrorHandler
import models.NormalMode
import models.journeydata.JourneyData
import models.journeydata.isaproducts.IsaProducts
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.{verify, when}
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.Writes
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Call, RequestHeader}
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers.*
import services.JourneyAnswersService
import views.html.isaproducts.PeerToPeerPlatformNumberView

import scala.concurrent.Future

class PeerToPeerPlatformNumberControllerSpec extends SpecBase {

  def onwardRoute: Call = Call("GET", "/foo")

  val formProvider       = new PeerToPeerPlatformNumberFormProvider()
  val form: Form[String] = formProvider.apply(testString)

  lazy val peerToPeerPlatformNumberRoute: String = PeerToPeerPlatformNumberController.onPageLoad(NormalMode).url

  val validAnswer                   = "123456"
  val validJourneyData: JourneyData =
    JourneyData(testGroupId, testString, isaProducts = Some(IsaProducts(p2pPlatform = Some(testString))))

  "PeerToPeerPlatformNumber Controller" - {

    "GET" - {

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(journeyData = Some(validJourneyData)).build()

        running(application) {
          val request = FakeRequest(GET, peerToPeerPlatformNumberRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[PeerToPeerPlatformNumberView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, testString, NormalMode)(request, messages(application)).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val journeyData = validJourneyData.copy(isaProducts =
          validJourneyData.isaProducts.map(_.copy(p2pPlatformNumber = Some(validAnswer)))
        )

        val application = applicationBuilder(journeyData = Some(journeyData)).build()

        running(application) {
          implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, peerToPeerPlatformNumberRoute)

          val view = application.injector.instanceOf[PeerToPeerPlatformNumberView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(validAnswer), testString, NormalMode)(
            request,
            messages(application)
          ).toString
        }
      }

      "must return a Bad Request for a GET if no platform name found" in {

        val dataMissingName = validJourneyData.copy(isaProducts = Some(IsaProducts(p2pPlatform = None)))

        val application = applicationBuilder(journeyData = Some(dataMissingName)).build()

        running(application) {
          val request = FakeRequest(GET, peerToPeerPlatformNumberRoute)

          await(route(application, request).value)

          verify(mockErrorHandler).badRequest(any[RequestHeader])
        }
      }
    }

    "POST" - {

      "must redirect to the next page when valid data is submitted" in {

        val expectedJourneyData = IsaProducts(p2pPlatform = Some(testString), p2pPlatformNumber = Some(validAnswer))

        when(
          mockJourneyAnswersService
            .update(eqTo(expectedJourneyData), eqTo(testGroupId))(any[Writes[IsaProducts]], any)
        ) thenReturn Future.successful(expectedJourneyData)

        val application =
          applicationBuilder(journeyData = Some(validJourneyData))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[JourneyAnswersService].toInstance(mockJourneyAnswersService)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, peerToPeerPlatformNumberRoute)
              .withFormUrlEncodedBody(("value", validAnswer))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(journeyData = Some(validJourneyData)).build()

        running(application) {
          implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, peerToPeerPlatformNumberRoute)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[PeerToPeerPlatformNumberView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, testString, NormalMode).toString
        }
      }

      "must return a Bad Request if no platform name found" in {

        val dataMissingName = validJourneyData.copy(isaProducts = Some(IsaProducts(p2pPlatform = None)))

        val application = applicationBuilder(
          journeyData = Some(dataMissingName),
          bind[JourneyAnswersService].toInstance(mockJourneyAnswersService)
        ).build()

        running(application) {
          val request = FakeRequest(POST, peerToPeerPlatformNumberRoute)

          await(route(application, request).value)

          verify(mockErrorHandler).badRequest(any[RequestHeader])
        }
      }

      "must return Internal Server Error when there is an issue storing the answer" in {

        when(
          mockJourneyAnswersService.update(any[IsaProducts], any[String])(any[Writes[IsaProducts]], any)
        ) thenReturn Future.failed(new Exception)

        val application =
          applicationBuilder(journeyData = Some(validJourneyData))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[JourneyAnswersService].toInstance(mockJourneyAnswersService)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, peerToPeerPlatformNumberRoute)
              .withFormUrlEncodedBody(("value", validAnswer))

          await(route(application, request).value)

          verify(mockErrorHandler).internalServerError(any[RequestHeader])
        }
      }
    }
  }
}
