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
import models.journeydata.JourneyData
import models.journeydata.liaisonofficers.LiaisonOfficerCommunication.{ByEmail, ByPhone}
import models.journeydata.liaisonofficers.{LiaisonOfficer, LiaisonOfficerCommunication, LiaisonOfficers}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.checkAnswers.liaisonofficers.*
import viewmodels.govuk.summarylist.*
import views.html.liaisonofficers.LoCheckYourAnswersView

class LoCheckYourAnswersControllerSpec extends SpecBase {

  private val existingId = "existing-id-123"
  private val otherId    = "other-id-123"

  lazy val routeUrl: String = routes.LoCheckYourAnswersController.onPageLoad(existingId).url

  "LoCheckYourAnswersController" - {

    "must return OK and the correct view for a GET when the liaison officer exists" in {

      val liaisonOfficer =
        LiaisonOfficer(
          id = existingId,
          fullName = Some("Jane Smith"),
          email = Some("jane@example.com"),
          phoneNumber = Some("01234567890"),
          communication = Set(ByEmail, ByPhone)
        )

      val journeyData =
        JourneyData(
          groupId = testGroupId,
          enrolmentId = testString,
          liaisonOfficers = Some(
            LiaisonOfficers(
              Seq(
                LiaisonOfficer(
                  id = otherId,
                  fullName = Some("Other Person"),
                  email = Some("other@example.com"),
                  phoneNumber = Some("01111111111"),
                  communication = Set(ByEmail)
                ),
                liaisonOfficer
              )
            )
          )
        )

      val application = applicationBuilder(journeyData = Some(journeyData)).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)
        val result  = route(application, request).value

        val view = application.injector.instanceOf[LoCheckYourAnswersView]

        val expectedRows =
          Seq(
            LiaisonOfficerNameSummary.row(liaisonOfficer),
            LiaisonOfficerEmailSummary.row(liaisonOfficer),
            LiaisonOfficerPhoneNumberSummary.row(liaisonOfficer),
            LiaisonOfficerCommunicationSummary.row(liaisonOfficer)
          ).flatten

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(SummaryListViewModel(expectedRows))(
          request,
          messages(application)
        ).toString
      }
    }
  }
}
