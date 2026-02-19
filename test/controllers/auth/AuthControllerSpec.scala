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

package controllers.auth

import base.SpecBase
import config.FrontendAppConfig
import models.session.Session
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository

import java.net.URLEncoder
import java.time.Instant
import scala.concurrent.Future

class AuthControllerSpec extends SpecBase with MockitoSugar {

  private val appConfig = injector.instanceOf[FrontendAppConfig]

  "signOut" - {

    "must clear the session for the current user and redirect to sign out when updates were made" in {
      val session               = Session(testCredentials.providerId, false, true, Instant.now)
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.findAndDelete(any[String])).thenReturn(Future.successful(Some(session)))

      val application =
        applicationBuilder(None)
          .overrides(inject.bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.AuthController.signOut().url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe s"${appConfig.signOutUrl}?continue=${URLEncoder.encode(controllers.auth.routes.SignedOutController.signOut().url, "UTF-8")}"
        verify(mockSessionRepository).findAndDelete(testCredentials.providerId)
      }
    }

    "must clear the session for the current user and redirect to sign out when updates were NOT made" in {
      val session               = Session(testCredentials.providerId, false, false, Instant.now)
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.findAndDelete(any[String])).thenReturn(Future.successful(Some(session)))

      val application =
        applicationBuilder(None)
          .overrides(inject.bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.AuthController.signOut().url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe s"${appConfig.signOutUrl}?continue=${URLEncoder.encode(controllers.auth.routes.SignedOutController.signOutAnswersNotSaved().url, "UTF-8")}"
        verify(mockSessionRepository).findAndDelete(testCredentials.providerId)
      }
    }
  }
}
