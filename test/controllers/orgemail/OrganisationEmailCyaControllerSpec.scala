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

package controllers.orgemail

import base.SpecBase
import controllers.orgemail.routes.{EmailVerificationCodeController, OrganisationEmailAddressController, OrganisationEmailCyaController}
import models.NormalMode
import models.journeydata.{JourneyData, OrganisationEmail}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.checkAnswers.OrganisationEmailSummary
import viewmodels.govuk.summarylist.SummaryListViewModel
import views.html.orgemail.OrganisationEmailCyaView

class OrganisationEmailCyaControllerSpec extends SpecBase {

  private val email = "test@example.com"

  lazy val routeUrl: String =
    OrganisationEmailCyaController.onPageLoad().url

  "OrganisationEmailCyaController" - {

    "must return OK and the correct view when organisation email is present and verified" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          organisationEmail = Some(
            OrganisationEmail(
              organisationEmail = Some(email),
              verified = Some(true)
            )
          )
        )

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[OrganisationEmailCyaView]

        val expectedRows =
          Seq(OrganisationEmailSummary.row(email))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(SummaryListViewModel(expectedRows))(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to organisation email address page when organisation email section is missing" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          organisationEmail = None
        )

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual OrganisationEmailAddressController
          .onPageLoad(NormalMode)
          .url
      }
    }

    "must redirect to organisation email address page when organisation email is missing" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          organisationEmail = Some(
            OrganisationEmail(
              organisationEmail = None,
              verified = Some(true)
            )
          )
        )

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual OrganisationEmailAddressController
          .onPageLoad(NormalMode)
          .url
      }
    }

    "must redirect to email verification code page when organisation email is present but not verified" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          organisationEmail = Some(
            OrganisationEmail(
              organisationEmail = Some(email),
              verified = Some(false)
            )
          )
        )

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual EmailVerificationCodeController
          .onPageLoad(NormalMode)
          .url
      }
    }

    "must redirect to email verification code page when organisation email is present but verified flag is missing" in {

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          organisationEmail = Some(
            OrganisationEmail(
              organisationEmail = Some(email),
              verified = None
            )
          )
        )

      val application =
        applicationBuilder(journeyData = Some(journeyData))
          .build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual EmailVerificationCodeController
          .onPageLoad(NormalMode)
          .url
      }
    }
  }
}
