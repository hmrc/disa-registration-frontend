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

package controllers.orgdetails

import base.SpecBase
import controllers.orgdetails.routes.*
import controllers.routes.*
import models.journeydata.{CorrespondenceAddress, JourneyData, OrganisationDetails}
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.orgdetails.ConfirmCorrespondenceAddressView

class ConfirmCorrespondenceAddressControllerSpec extends SpecBase with MockitoSugar {

  private val address =
    CorrespondenceAddress(
      addressLine1 = Some("1 Test Street"),
      addressLine2 = Some("London"),
      addressLine3 = None,
      postCode = Some("AA1 1AA")
    )

  private val journeyDataWithAddress: JourneyData =
    JourneyData(
      groupId = testGroupId,
      enrolmentId = testString,
      organisationDetails = Some(
        OrganisationDetails(
          correspondenceAddress = Some(address)
        )
      )
    )

  private val journeyDataWithoutAddress: JourneyData =
    JourneyData(
      groupId = testGroupId,
      enrolmentId = testString,
      organisationDetails = Some(
        OrganisationDetails(
          correspondenceAddress = None
        )
      )
    )

  private val routeUrl = ConfirmCorrespondenceAddressController.onPageLoad().url

  "ConfirmCorrespondenceAddressController" - {

    "must return OK and display the confirm correspondence address view when address exists" in {

      val application =
        applicationBuilder(journeyData = Some(journeyDataWithAddress)).build()

      running(application) {

        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ConfirmCorrespondenceAddressView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual
          view(address)(request, messages(application)).toString
      }
    }

    "must redirect to TaskList when correspondence address is missing" in {

      val application =
        applicationBuilder(journeyData = Some(journeyDataWithoutAddress)).build()

      running(application) {

        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          TaskListController.onPageLoad().url
      }
    }

    "must redirect to TaskList when organisation details are missing" in {

      val application =
        applicationBuilder(journeyData = None).build()

      running(application) {

        val request = FakeRequest(GET, routeUrl)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          TaskListController.onPageLoad().url
      }
    }
  }
}
