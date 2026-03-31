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

package controllers.liaisonofficers

import base.SpecBase
import controllers.liaisonofficers.routes.LiaisonOfficerCommunicationController
import controllers.routes.IndexController
import forms.LiaisonOfficerCommunicationFormProvider
import models.journeydata.JourneyData
import models.journeydata.liaisonofficers.LiaisonOfficerCommunication.{ByEmail, ByPhone}
import models.journeydata.liaisonofficers.{LiaisonOfficer, LiaisonOfficerCommunication, LiaisonOfficers}
import models.{CheckMode, NormalMode}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{atMostOnce, verify, when}
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.Writes
import play.api.mvc.{Call, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.liaisonofficers.LiaisonOfficerCommunicationView

import scala.concurrent.Future

class LiaisonOfficerCommunicationControllerSpec extends SpecBase {

  def onwardRoute: Call = Call("GET", "/foo")

  private val existingId            = "existing-id-123"
  private val otherId               = "other-id-123"
  private val existingName          = "Jane Smith"
  private val existingCommunication = Set[LiaisonOfficerCommunication](ByEmail)
  private val updatedCommunication  = Set[LiaisonOfficerCommunication](ByEmail, ByPhone)

  lazy val routeUrl: String  = LiaisonOfficerCommunicationController.onPageLoad(existingId, NormalMode).url
  lazy val submitUrl: String = LiaisonOfficerCommunicationController.onSubmit(existingId, NormalMode).url

  val formProvider: LiaisonOfficerCommunicationFormProvider = new LiaisonOfficerCommunicationFormProvider()
  val form: Form[Set[LiaisonOfficerCommunication]]          = formProvider()

  "LiaisonOfficerCommunicationController" - {

    "must return OK and the correct view for a GET when the liaison officer exists with no communication preferences selected" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(
            LiaisonOfficers(
              Seq(
                LiaisonOfficer(existingId, Some(existingName), communication = Set.empty)
              )
            )
          )
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[LiaisonOfficerCommunicationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(existingId, existingName, form.fill(Set.empty), NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when previously answered" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(
            LiaisonOfficers(
              Seq(
                LiaisonOfficer(otherId, Some("Other Person"), communication = Set(ByPhone)),
                LiaisonOfficer(existingId, Some(existingName), communication = existingCommunication)
              )
            )
          )
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[LiaisonOfficerCommunicationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(existingId, existingName, form.fill(existingCommunication), NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Index on a GET when the liaison officer does not exist" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(
            LiaisonOfficers(
              Seq(
                LiaisonOfficer(otherId, Some("Other Person"), communication = Set(ByPhone))
              )
            )
          )
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual IndexController.onPageLoad().url
      }
    }

    "must redirect to Index on a GET when the liaison officer has no name" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(
            LiaisonOfficers(
              Seq(
                LiaisonOfficer(existingId, None, communication = existingCommunication)
              )
            )
          )
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual IndexController.onPageLoad().url
      }
    }

    "must return BadRequest and errors when invalid data is submitted and liaison officer details can be found" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(
            LiaisonOfficers(
              Seq(
                LiaisonOfficer(existingId, Some(existingName), communication = Set.empty)
              )
            )
          )
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value[0]" -> "")

        val boundForm = form.bind(Map("value[0]" -> ""))

        val view = application.injector.instanceOf[LiaisonOfficerCommunicationView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(existingId, existingName, boundForm, NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Index when invalid data is submitted and liaison officer details cannot be found" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value[0]" -> "")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual IndexController.onPageLoad().url
      }
    }

    "must redirect to the next page when valid data is submitted and existing liaison officer is updated" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(
            LiaisonOfficers(
              Seq(
                LiaisonOfficer(otherId, Some("Other Person"), communication = Set(ByPhone)),
                LiaisonOfficer(existingId, Some(existingName), communication = existingCommunication)
              )
            )
          )
        )

      val expectedSection =
        LiaisonOfficers(
          Seq(
            LiaisonOfficer(otherId, Some("Other Person"), communication = Set(ByPhone)),
            LiaisonOfficer(existingId, Some(existingName), communication = updatedCommunication)
          )
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(
              "value[0]" -> "byEmail",
              "value[1]" -> "byPhone"
            )

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to Index when valid data is submitted and the section is absent" in {

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(
              "value[0]" -> "byEmail",
              "value[1]" -> "byPhone"
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual IndexController.onPageLoad().url
      }
    }

    "must redirect to Index when valid data is submitted and the liaison officer id is not present in the section" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(
            LiaisonOfficers(
              Seq(
                LiaisonOfficer(otherId, Some("Other Person"), communication = Set(ByPhone))
              )
            )
          )
        )

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(
              "value[0]" -> "byEmail",
              "value[1]" -> "byPhone"
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual IndexController.onPageLoad().url
      }
    }

    "must return Internal Server Error when JourneyAnswersService.update fails" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(
            LiaisonOfficers(
              Seq(
                LiaisonOfficer(existingId, Some(existingName), communication = existingCommunication)
              )
            )
          )
        )

      when(
        mockJourneyAnswersService
          .update(any[LiaisonOfficers], any[String], any[String])(any[Writes[LiaisonOfficers]], any)
      ).thenReturn(Future.failed(new Exception("fubar")))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(
              "value[0]" -> "byEmail",
              "value[1]" -> "byPhone"
            )

        await(route(application, request).value)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }

    "must render view with CheckMode on a GET" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(
            LiaisonOfficers(
              Seq(
                LiaisonOfficer(existingId, Some(existingName), communication = existingCommunication)
              )
            )
          )
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, LiaisonOfficerCommunicationController.onPageLoad(existingId, CheckMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[LiaisonOfficerCommunicationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(existingId, existingName, form.fill(existingCommunication), CheckMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must submit with CheckMode and redirect" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(
            LiaisonOfficers(
              Seq(
                LiaisonOfficer(existingId, Some(existingName), communication = existingCommunication)
              )
            )
          )
        )

      val expectedSection =
        LiaisonOfficers(
          Seq(
            LiaisonOfficer(existingId, Some(existingName), communication = updatedCommunication)
          )
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, LiaisonOfficerCommunicationController.onSubmit(existingId, CheckMode).url)
            .withFormUrlEncodedBody(
              "value[0]" -> "byEmail",
              "value[1]" -> "byPhone"
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }
  }
}
