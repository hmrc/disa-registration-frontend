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
import models.requests.IdentifierRequest
import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.{verify, when}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository

import scala.concurrent.Future

class KeepAliveControllerSpec extends SpecBase {

  "KeepAliveController" - {

    "must return OK for a GET and call keepAlive with the user's userId" in {

      when(mockSessionRepository.keepAlive(eqTo(testCredentials.providerId)))
        .thenReturn(Future.successful(()))

      val application = applicationBuilder(journeyData = None)
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request =
          IdentifierRequest(
            FakeRequest(GET, routes.KeepAliveController.keepAlive().url),
            testGroupId,
            testCredentials,
            testCredentialRoleUser
          )

        val result = route(application, request).value

        status(result) mustEqual OK

        verify(mockSessionRepository).keepAlive(eqTo(testCredentials.providerId))
      }
    }
  }
}
