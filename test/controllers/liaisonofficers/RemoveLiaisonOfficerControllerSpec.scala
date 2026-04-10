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
import controllers.routes.IndexController
import forms.YesNoAnswerFormProvider
import models.YesNoAnswer.{No, Yes}
import models.journeydata.JourneyData
import models.journeydata.liaisonofficers.{LiaisonOfficer, LiaisonOfficerCommunication, LiaisonOfficers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{atMostOnce, verify, when}
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.Writes
import play.api.mvc.{Call, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.liaisonofficers.RemoveLiaisonOfficerView

import scala.concurrent.Future

class RemoveLiaisonOfficerControllerSpec extends SpecBase {

  def onwardRoute: Call = Call("GET", "/foo")

  private val existingId   = "existing-id-123"
  private val otherId      = "other-id-123"
  private val existingName = "Jane Smith"

  lazy val routeUrl: String  = routes.RemoveLiaisonOfficerController.onPageLoad(existingId).url
  lazy val submitUrl: String = routes.RemoveLiaisonOfficerController.onSubmit(existingId).url

  val formProvider: YesNoAnswerFormProvider = new YesNoAnswerFormProvider()
  val form: Form[models.YesNoAnswer]        = formProvider("removeLiaisonOfficer.error.required")

  private val existingLiaisonOfficer =
    LiaisonOfficer(
      id = existingId,
      fullName = Some(existingName),
      email = Some("jane@example.com"),
      phoneNumber = Some("07123456789"),
      communication = Set(LiaisonOfficerCommunication.ByEmail)
    )

  private val otherLiaisonOfficer =
    LiaisonOfficer(
      id = otherId,
      fullName = Some("Other Person"),
      email = Some("other@example.com"),
      phoneNumber = Some("07987654321"),
      communication = Set(LiaisonOfficerCommunication.ByPhone)
    )

  "RemoveLiaisonOfficerController" - {

    "must return OK and the correct view for a GET when the liaison officer exists" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(LiaisonOfficers(Seq(existingLiaisonOfficer, otherLiaisonOfficer)))
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemoveLiaisonOfficerView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(existingId, existingName, form)(request, messages(application)).toString
      }
    }

    "must redirect to Index on a GET when the liaison officer does not exist" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(LiaisonOfficers(Seq(otherLiaisonOfficer)))
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual IndexController.onPageLoad().url
      }
    }

    "must redirect to Index on a GET when the section is absent" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual IndexController.onPageLoad().url
      }
    }

    "must return BadRequest and errors when invalid data is submitted" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(LiaisonOfficers(Seq(existingLiaisonOfficer, otherLiaisonOfficer)))
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> "")

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[RemoveLiaisonOfficerView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(existingId, existingName, boundForm)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when Yes is submitted and the liaison officer is removed" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(LiaisonOfficers(Seq(existingLiaisonOfficer, otherLiaisonOfficer)))
        )

      val expectedSection =
        LiaisonOfficers(Seq(otherLiaisonOfficer))

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
            .withFormUrlEncodedBody("value" -> Yes.toString)

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the next page when No is submitted and the section is unchanged" in {

      val existingSection =
        LiaisonOfficers(Seq(existingLiaisonOfficer, otherLiaisonOfficer))

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(existingSection)
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(existingSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)
      ).thenReturn(Future.successful(existingSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> No.toString)

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(existingSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the next page when Yes is submitted and removing the liaison officer leaves an empty section" in {

      val existingSection =
        LiaisonOfficers(Seq(existingLiaisonOfficer))

      val updatedSection =
        LiaisonOfficers(Seq.empty)

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(existingSection)
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(updatedSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)
      ).thenReturn(Future.successful(updatedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> Yes.toString)

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(updatedSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to Index when data is submitted and the liaison officer does not exist" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(LiaisonOfficers(Seq(otherLiaisonOfficer)))
        )

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> Yes.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual IndexController.onPageLoad().url
      }
    }

    "must redirect to Index when data is submitted and the section is absent" in {

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> Yes.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual IndexController.onPageLoad().url
      }
    }

    "must return Internal Server Error when JourneyAnswersService.update fails" in {

      val existingSection =
        LiaisonOfficers(Seq(existingLiaisonOfficer, otherLiaisonOfficer))

      val updatedSection =
        LiaisonOfficers(Seq(otherLiaisonOfficer))

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(existingSection)
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(updatedSection), any[String], any[String])(any[Writes[LiaisonOfficers]], any)
      ).thenReturn(Future.failed(new Exception("fubar")))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody("value" -> Yes.toString)

        await(route(application, request).value)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }
  }
}
