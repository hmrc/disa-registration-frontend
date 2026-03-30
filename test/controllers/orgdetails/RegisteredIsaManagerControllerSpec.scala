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
import controllers.orgdetails
import forms.RegisteredIsaManagerFormProvider
import models.NormalMode
import models.journeydata.{JourneyData, OrganisationDetails}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.{Call, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.orgdetails.RegisteredIsaManagerView

import scala.concurrent.Future

class RegisteredIsaManagerControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  val formProvider        = new RegisteredIsaManagerFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val routePath: String =
    orgdetails.routes.RegisteredIsaManagerController.onPageLoad(NormalMode).url

  "onPageLoad" - {

    "must return OK with empty form when no existing answer" in {

      val app = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(app) {
        val request = FakeRequest(GET, routePath)
        val result  = route(app, request).value
        val view    = app.injector.instanceOf[RegisteredIsaManagerView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual
          view(form, NormalMode)(request, messages(app)).toString
      }
    }

    "must return empty form when organisationDetails exists but answer is None" in {

      val jd = JourneyData(
        groupId = testGroupId,
        enrolmentId = testString,
        organisationDetails = Some(
          OrganisationDetails(registeredToManageIsa = None)
        )
      )

      val app = applicationBuilder(journeyData = Some(jd)).build()

      running(app) {
        val request = FakeRequest(GET, routePath)
        val result  = route(app, request).value
        val view    = app.injector.instanceOf[RegisteredIsaManagerView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual
          view(form, NormalMode)(request, messages(app)).toString
      }
    }

    "must pre-populate form when answer exists" in {

      val jd = JourneyData(
        groupId = testGroupId,
        enrolmentId = testString,
        organisationDetails = Some(
          OrganisationDetails(registeredToManageIsa = Some(true))
        )
      )

      val app = applicationBuilder(journeyData = Some(jd)).build()

      running(app) {
        val request = FakeRequest(GET, routePath)
        val result  = route(app, request).value
        val view    = app.injector.instanceOf[RegisteredIsaManagerView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual
          view(form.fill(true), NormalMode)(request, messages(app)).toString
      }
    }
  }

  "onSubmit" - {

    "must create OrganisationDetails when none exists (true)" in {

      when(
        mockJourneyAnswersService
          .update(any[OrganisationDetails], any[String], any[String])(any(), any)
      ).thenReturn(Future.successful(OrganisationDetails(Some(true))))

      val jd = JourneyData(
        groupId = testGroupId,
        enrolmentId = testString,
        organisationDetails = None
      )

      val app =
        applicationBuilder(journeyData = Some(jd))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(app) {
        val request =
          FakeRequest(POST, routePath)
            .withFormUrlEncodedBody("value" -> "true")

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        verify(mockJourneyAnswersService).update(
          argThat[OrganisationDetails](_.registeredToManageIsa.contains(true)),
          any[String],
          any[String]
        )(any(), any())
      }
    }

    "must create OrganisationDetails when none exists (false)" in {

      when(
        mockJourneyAnswersService
          .update(any[OrganisationDetails], any[String], any[String])(any(), any)
      ).thenReturn(Future.successful(OrganisationDetails(Some(false))))

      val jd = JourneyData(
        groupId = testGroupId,
        enrolmentId = testString,
        organisationDetails = None
      )

      val app =
        applicationBuilder(journeyData = Some(jd))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(app) {
        val request =
          FakeRequest(POST, routePath)
            .withFormUrlEncodedBody("value" -> "false")

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        verify(mockJourneyAnswersService).update(
          argThat[OrganisationDetails](_.registeredToManageIsa.contains(false)),
          any[String],
          any[String]
        )(any(), any())
      }
    }

    "must update existing OrganisationDetails and preserve fields" in {

      val existing = OrganisationDetails(
        registeredToManageIsa = Some(false),
        tradingName = Some("keep-me")
      )

      val jd = JourneyData(
        groupId = testGroupId,
        enrolmentId = testString,
        organisationDetails = Some(existing)
      )

      when(
        mockJourneyAnswersService
          .update(any[OrganisationDetails], any[String], any[String])(any(), any)
      ).thenReturn(Future.successful(existing.copy(registeredToManageIsa = Some(true))))

      val app =
        applicationBuilder(journeyData = Some(jd))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(app) {
        val request =
          FakeRequest(POST, routePath)
            .withFormUrlEncodedBody("value" -> "true")

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        verify(mockJourneyAnswersService).update(
          argThat[OrganisationDetails](od =>
            od.registeredToManageIsa.contains(true) &&
              od.tradingName.contains("keep-me")
          ),
          any[String],
          any[String]
        )(any(), any())
      }
    }

    "must return BAD_REQUEST and render errors when invalid form submitted" in {

      val app = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      running(app) {
        val request =
          FakeRequest(POST, routePath)
            .withFormUrlEncodedBody("value" -> "")

        val boundForm = form.bind(Map("value" -> ""))

        val view = app.injector.instanceOf[RegisteredIsaManagerView]

        val result = route(app, request).value

        status(result) mustEqual BAD_REQUEST

        contentAsString(result) mustEqual
          view(boundForm, NormalMode)(request, messages(app)).toString
      }
    }

    "must return 500 when service fails" in {

      when(
        mockJourneyAnswersService
          .update(any[OrganisationDetails], any[String], any[String])(any(), any)
      ).thenReturn(Future.failed(new Exception("boom")))

      val app =
        applicationBuilder(journeyData = Some(emptyJourneyData))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(app) {
        val request =
          FakeRequest(POST, routePath)
            .withFormUrlEncodedBody("value" -> "true")

        await(route(app, request).value)

        verify(mockErrorHandler).internalServerError(any[RequestHeader])
      }
    }
  }
}
