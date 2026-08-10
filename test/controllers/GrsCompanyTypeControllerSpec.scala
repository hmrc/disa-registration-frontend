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

package controllers

import base.SpecBase
import forms.GrsCompanyTypeFormProvider
import models.grs.GrsCompanyType
import models.journeydata.BusinessVerification
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.GrsCompanyTypeView

import scala.concurrent.Future

class GrsCompanyTypeControllerSpec extends SpecBase {

  lazy val routeUrl: String  = routes.GrsCompanyTypeController.onPageLoad().url
  lazy val submitUrl: String = routes.GrsCompanyTypeController.onSubmit().url

  val formProvider               = new GrsCompanyTypeFormProvider()
  val form: Form[GrsCompanyType] = formProvider()

  "GrsCompanyTypeController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(journeyData = None).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[GrsCompanyTypeView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form)(request, messages(application)).toString
      }
    }

    "must prepopulate the form when a company type has already been selected" in {

      val journeyData = emptyJourneyData.copy(
        businessVerification = Some(testBV.copy(companyType = Some(GrsCompanyType.GeneralPartnership)))
      )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[GrsCompanyTypeView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(form.fill(GrsCompanyType.GeneralPartnership))(request, messages(application)).toString
      }
    }

    "must return BadRequest and errors when no option is selected" in {

      val application = applicationBuilder(journeyData = None).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))
        val view      = application.injector.instanceOf[GrsCompanyTypeView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm)(request, messages(application)).toString
      }
    }

    "must persist the selected company type and redirect to GrsStartController when valid data is submitted" in {

      when(mockJourneyAnswersService.update(any[BusinessVerification], any[String], any[String])(any(), any()))
        .thenReturn(Future.successful(BusinessVerification(None, None, None, None, None, None, None)))

      val application = applicationBuilder(journeyData = None).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", GrsCompanyType.LimitedCompany.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.GrsStartController.onPageLoad().url

        verify(mockJourneyAnswersService).update(
          eqTo(BusinessVerification(None, None, None, None, None, None, None, Some(GrsCompanyType.LimitedCompany))),
          any[String],
          any[String]
        )(any(), any())
      }
    }

    "must preserve existing business verification data when persisting a new company type" in {

      val journeyData = emptyJourneyData.copy(businessVerification = Some(testBV))

      when(mockJourneyAnswersService.update(any[BusinessVerification], any[String], any[String])(any(), any()))
        .thenReturn(Future.successful(testBV))

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitUrl)
            .withFormUrlEncodedBody(("value", GrsCompanyType.LimitedCompany.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        verify(mockJourneyAnswersService).update(
          eqTo(testBV.copy(companyType = Some(GrsCompanyType.LimitedCompany))),
          any[String],
          any[String]
        )(any(), any())
      }
    }
  }
}
