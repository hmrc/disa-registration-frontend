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
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository

import java.net.URLEncoder
import java.time.Instant
import scala.concurrent.Future

class AuthControllerSpec extends SpecBase with MockitoSugar {

  private val appConfig = injector.instanceOf[FrontendAppConfig]

  private def expectedRedirect(continueUrl: String): String =
    s"${appConfig.signOutUrl}?continue=${URLEncoder.encode(continueUrl, "UTF-8")}"

  "signOut" - {

    "must find and delete the session and redirect to answer-saved sign out when answers were saved" in {
      val session = Session(
        testCredentials.providerId,
        auditContinuationEventSent = false,
        updatesInThisSession = true,
        lastSeen = Instant.now
      )

      when(mockSessionRepository.findAndDelete(any[String]))
        .thenReturn(Future.successful(Some(session)))

      val application =
        applicationBuilder(None)
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.AuthController.signOut().url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe expectedRedirect(
          controllers.auth.routes.SignedOutController.signOut(true).url
        )
        verify(mockSessionRepository).findAndDelete(testCredentials.providerId)
      }
    }

    "must find and delete the session and redirect to regular sign out when answers were NOT saved" in {
      val session = Session(
        testCredentials.providerId,
        auditContinuationEventSent = false,
        updatesInThisSession = false,
        lastSeen = Instant.now
      )

      when(mockSessionRepository.findAndDelete(any[String]))
        .thenReturn(Future.successful(Some(session)))

      val application =
        applicationBuilder(None)
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.AuthController.signOut().url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe expectedRedirect(
          controllers.auth.routes.SignedOutController.signOut(false).url
        )
        verify(mockSessionRepository).findAndDelete(testCredentials.providerId)
      }
    }

    "must redirect to sign out when no session exists for the user" in {
      when(mockSessionRepository.findAndDelete(any[String]))
        .thenReturn(Future.successful(None))

      val application =
        applicationBuilder(None)
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.AuthController.signOut().url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe expectedRedirect(
          controllers.auth.routes.SignedOutController.signOut(false).url
        )
        verify(mockSessionRepository).findAndDelete(testCredentials.providerId)
      }
    }

    "must still redirect to sign out when findAndDelete fails" in {
      when(mockSessionRepository.findAndDelete(any[String]))
        .thenReturn(Future.failed(new RuntimeException("fubar")))

      val application =
        applicationBuilder(None)
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.AuthController.signOut().url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe expectedRedirect(
          controllers.auth.routes.SignedOutController.signOut(false).url
        )
        verify(mockSessionRepository).findAndDelete(testCredentials.providerId)
      }
    }
  }
}
