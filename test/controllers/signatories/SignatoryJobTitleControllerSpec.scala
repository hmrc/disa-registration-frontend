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

package controllers.signatories

import base.SpecBase
import controllers.signatories.routes.SignatoryJobTitleController
import controllers.routes.IndexController
import forms.SignatoryJobTitleFormProvider
import models.journeydata.JourneyData
import models.journeydata.signatories.{Signatories, Signatory}
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
import views.html.signatories.SignatoryJobTitleView

import scala.concurrent.Future

class SignatoryJobTitleControllerSpec extends SpecBase {

  def onwardRoute: Call = Call("GET", "/obligations/enrolment/isa")

  private val existingId = "existing-id-123"
  private val otherId = "other-id-123"
  private val existingName = "Jane Smith"
  private val existingJobTitle = "Existing Job Title"
  private val newJobTitle = "New Job Title"

  lazy val routeUrl: String = SignatoryJobTitleController.onPageLoad(existingId, NormalMode).url
  lazy val submitUrl: String = SignatoryJobTitleController.onSubmit(existingId, NormalMode).url

  val formProvider: SignatoryJobTitleFormProvider = new SignatoryJobTitleFormProvider()
  val form: Form[String] = formProvider()

  "SignatoryJobTitleController" - {

    "must return OK and the correct view for a GET when the signatory exists and has no job title" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(
            Signatories(
              Seq(
                Signatory(existingId, Some(existingName), jobTitle = None)
              )
            )
          )
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SignatoryJobTitleView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(existingId, existingName, form, NormalMode)(
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
          signatories = Some(
            Signatories(
              Seq(
                Signatory(otherId, Some("Other Person"), jobTitle = Some("Job Title")),
                Signatory(existingId, Some(existingName), jobTitle = Some(existingJobTitle))
              )
            )
          )
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SignatoryJobTitleView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(existingId, existingName, form.fill(existingJobTitle), NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Index on a GET when the signatory does not exist" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(
            Signatories(
              Seq(
                Signatory(otherId, Some("Other Person"), jobTitle = Some("Job Title"))
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

    "must redirect to Index on a GET when the signatory has no name" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(
            Signatories(
              Seq(
                Signatory(existingId, None, jobTitle = Some(existingJobTitle))
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

    "must return BadRequest and errors when invalid data is submitted and signatory details can be found" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(
            Signatories(
              Seq(
                Signatory(existingId, Some(existingName), jobTitle = None)
              )
            )
          )
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[SignatoryJobTitleView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(existingId, existingName, boundForm, NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Index when invalid data is submitted and signatory details cannot be found" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual IndexController.onPageLoad().url
      }
    }

    "must redirect to the next page when valid data is submitted and existing signatory is updated" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(
            Signatories(
              Seq(
                Signatory(otherId, Some("Other Person"), jobTitle = Some("Job Title")),
                Signatory(existingId, Some(existingName), jobTitle = Some(existingJobTitle))
              )
            )
          )
        )

      val expectedSection =
        Signatories(
          Seq(
            Signatory(otherId, Some("Other Person"), jobTitle = Some("Job Title")),
            Signatory(existingId, Some(existingName), jobTitle = Some(newJobTitle))
          )
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[Signatories]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", newJobTitle))

        val result = route(application, request).value

        verify(mockJourneyAnswersService, atMostOnce)
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[Signatories]], any)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to Index when valid data is submitted and the section is absent" in {

      val application =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", newJobTitle))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual IndexController.onPageLoad().url
      }
    }

    "must redirect to Index when valid data is submitted and the signatory id is not present in the section" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(
            Signatories(
              Seq(
                Signatory(otherId, Some("Other Person"), jobTitle = Some("Job Title"))
              )
            )
          )
        )

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", newJobTitle))

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
          signatories = Some(
            Signatories(
              Seq(
                Signatory(existingId, Some(existingName), jobTitle = Some(existingJobTitle))
              )
            )
          )
        )

      when(
        mockJourneyAnswersService
          .update(any[Signatories], any[String], any[String])(any[Writes[Signatories]], any)
      ).thenReturn(Future.failed(new Exception("fubar")))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", newJobTitle))

        await(route(application, request).value)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }

    "must render view with CheckMode on a GET" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          signatories = Some(
            Signatories(
              Seq(
                Signatory(existingId, Some(existingName), jobTitle = Some(existingJobTitle))
              )
            )
          )
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, SignatoryJobTitleController.onPageLoad(existingId, CheckMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SignatoryJobTitleView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(existingId, existingName, form.fill(existingJobTitle), CheckMode)(
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
          signatories = Some(
            Signatories(
              Seq(
                Signatory(existingId, Some(existingName), jobTitle = Some(existingJobTitle))
              )
            )
          )
        )

      val expectedSection =
        Signatories(
          Seq(
            Signatory(existingId, Some(existingName), jobTitle = Some(newJobTitle))
          )
        )

      when(
        mockJourneyAnswersService
          .update(eqTo(expectedSection), any[String], any[String])(any[Writes[Signatories]], any)
      ).thenReturn(Future.successful(expectedSection))

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, SignatoryJobTitleController.onSubmit(existingId, CheckMode).url)
            .withFormUrlEncodedBody(("value", newJobTitle))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }
  }
}
