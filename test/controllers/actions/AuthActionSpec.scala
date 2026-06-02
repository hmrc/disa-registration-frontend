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

package controllers.actions

import base.SpecBase
import config.FrontendAppConfig
import controllers.routes
import org.mockito.ArgumentMatchers.{any => anyArg}
import org.mockito.Mockito.{times, verify, when}
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class AuthActionSpec extends SpecBase {

  class Harness(authAction: IdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction(_ => Results.Ok)
  }

  val config: FrontendAppConfig = injector.instanceOf[FrontendAppConfig]

  private def authenticatedIdentifierAction(
    authConnector: AuthConnector,
    appConfig: FrontendAppConfig,
    bodyParsers: BodyParsers.Default
  ): AuthenticatedIdentifierAction =
    new AuthenticatedIdentifierAction(
      authConnector = authConnector,
      config = appConfig,
      taxEnrolmentsService = mockTaxEnrolmentsService,
      errorHandler = mockErrorHandler,
      sessionRepository = mockSessionRepository,
      parser = bodyParsers
    )

  "Auth Action" - {

    "when the user hasn't logged in" - {

      "must redirect the user to log in" in {

        val application = applicationBuilder(journeyData = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = authenticatedIdentifierAction(
            failingAuthConnector(new MissingBearerToken),
            appConfig,
            bodyParsers
          )

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user's session has expired" - {

      "must redirect the user to log in" in {

        val application = applicationBuilder(journeyData = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = authenticatedIdentifierAction(
            failingAuthConnector(new BearerTokenExpired),
            appConfig,
            bodyParsers
          )

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user doesn't have sufficient enrolments" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(journeyData = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = authenticatedIdentifierAction(
            failingAuthConnector(new InsufficientEnrolments),
            appConfig,
            bodyParsers
          )

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "the user doesn't have sufficient confidence level" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(journeyData = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = authenticatedIdentifierAction(
            failingAuthConnector(new InsufficientConfidenceLevel),
            appConfig,
            bodyParsers
          )

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "the user used an unaccepted auth provider" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(journeyData = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = authenticatedIdentifierAction(
            failingAuthConnector(new UnsupportedAuthProvider),
            appConfig,
            bodyParsers
          )

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "the user has an unsupported affinity group (Agent)" - {

      "must redirect the user to the unsupported affinity group page" in {

        val application = applicationBuilder(journeyData = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = authenticatedIdentifierAction(
            successfulAuthConnector(
              affinityGroup = Some(AffinityGroup.Agent),
              groupId = Some(testGroupId),
              credentials = Some(testCredentials),
              credentialRole = Some(testCredentialRoleUser)
            ),
            appConfig,
            bodyParsers
          )

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(
            routes.UnsupportedAffinityGroupController
              .onPageLoad(AffinityGroup.Agent.toString)
              .url
          )
        }
      }
    }

    "the user has an unsupported affinity group (Individual)" - {

      "must redirect the user to the unsupported affinity group page" in {

        val application = applicationBuilder(journeyData = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = authenticatedIdentifierAction(
            successfulAuthConnector(
              affinityGroup = Some(AffinityGroup.Individual),
              groupId = Some("group-id-123")
            ),
            appConfig,
            bodyParsers
          )

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(
            routes.UnsupportedAffinityGroupController
              .onPageLoad(AffinityGroup.Individual.toString)
              .url
          )
        }
      }
    }

    "the user has an unsupported credential role" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(journeyData = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = authenticatedIdentifierAction(
            failingAuthConnector(new UnsupportedCredentialRole),
            appConfig,
            bodyParsers
          )

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
        }
      }
    }

    "the user doesn't have a credential role" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(journeyData = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = authenticatedIdentifierAction(
            successfulAuthConnector(Some(testGroupId), Some(Organisation), Some(Credentials("", "")), None),
            appConfig,
            bodyParsers
          )

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
        }
      }
    }

    "the user doesn't have credentials" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(journeyData = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = authenticatedIdentifierAction(
            successfulAuthConnector(Some(testGroupId), Some(Organisation), None, Some(User)),
            appConfig,
            bodyParsers
          )

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
        }
      }
    }

    "must still allow the request to proceed when keepAlive fails" in {
      val application = applicationBuilder(journeyData = None).build()

      running(application) {
        val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
        val appConfig   = application.injector.instanceOf[FrontendAppConfig]

        when(mockSessionRepository.keepAlive(testCredentials.providerId))
          .thenReturn(Future.failed(new RuntimeException("fubar")))

        val authAction = authenticatedIdentifierAction(
          successfulAuthConnector(
            affinityGroup = Some(Organisation),
            groupId = Some(testGroupId),
            credentials = Some(testCredentials),
            credentialRole = Some(testCredentialRoleUser)
          ),
          appConfig,
          bodyParsers
        )

        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(FakeRequest())

        status(result) mustBe OK
        verify(mockSessionRepository, times(1)).keepAlive(testCredentials.providerId)
      }
    }

    "the user has already enrolled for Manage ISA" - {

      "must redirect to the organisation is enrolled page" in {

        val application = applicationBuilder(journeyData = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = authenticatedIdentifierAction(
            successfulAuthConnector(
              affinityGroup = Some(Organisation),
              groupId = Some(testGroupId),
              credentials = Some(testCredentials),
              credentialRole = Some(testCredentialRoleUser),
              allEnrolments = Enrolments(Set(Enrolment(appConfig.manageIsaEnrolmentKey)))
            ),
            appConfig,
            bodyParsers
          )

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest(GET, routes.TaskListController.onPageLoad().url))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.OrganisationIsEnrolledController.onPageLoad().url)
        }
      }

      "must allow the organisation is enrolled page to render" in {

        val application = applicationBuilder(journeyData = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = authenticatedIdentifierAction(
            successfulAuthConnector(
              affinityGroup = Some(Organisation),
              groupId = Some(testGroupId),
              credentials = Some(testCredentials),
              credentialRole = Some(testCredentialRoleUser),
              allEnrolments = Enrolments(Set(Enrolment(appConfig.manageIsaEnrolmentKey)))
            ),
            appConfig,
            bodyParsers
          )

          val controller = new Harness(authAction)
          val result     =
            controller.onPageLoad()(FakeRequest(GET, routes.OrganisationIsEnrolledController.onPageLoad().url))

          status(result) mustBe OK
        }
      }

      "must allow the confirmation page to render" in {

        val application = applicationBuilder(journeyData = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = authenticatedIdentifierAction(
            successfulAuthConnector(
              affinityGroup = Some(Organisation),
              groupId = Some(testGroupId),
              credentials = Some(testCredentials),
              credentialRole = Some(testCredentialRoleUser),
              allEnrolments = Enrolments(Set(Enrolment(appConfig.manageIsaEnrolmentKey)))
            ),
            appConfig,
            bodyParsers
          )

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest(GET, routes.ConfirmationController.onPageLoad().url))

          status(result) mustBe OK
        }
      }

      "must not redirect when the Manage ISA enrolment is not activated" in {

        val application = applicationBuilder(journeyData = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val inactiveEnrolment = Enrolment(
            key = appConfig.manageIsaEnrolmentKey,
            identifiers = Seq.empty,
            state = "NotYetActivated"
          )

          val authAction = authenticatedIdentifierAction(
            successfulAuthConnector(
              affinityGroup = Some(Organisation),
              groupId = Some(testGroupId),
              credentials = Some(testCredentials),
              credentialRole = Some(testCredentialRoleUser),
              allEnrolments = Enrolments(Set(inactiveEnrolment))
            ),
            appConfig,
            bodyParsers
          )

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest(GET, routes.TaskListController.onPageLoad().url))

          status(result) mustBe OK
        }
      }

      "must allow sign out when the Manage ISA enrolment is activated" in {

        val application = applicationBuilder(journeyData = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = authenticatedIdentifierAction(
            successfulAuthConnector(
              affinityGroup = Some(Organisation),
              groupId = Some(testGroupId),
              credentials = Some(testCredentials),
              credentialRole = Some(testCredentialRoleUser),
              allEnrolments = Enrolments(Set(Enrolment(appConfig.manageIsaEnrolmentKey)))
            ),
            appConfig,
            bodyParsers
          )

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest(GET, controllers.auth.routes.AuthController.signOut().url))

          status(result) mustBe OK
        }
      }
    }

    "the user has a Manage ISA subscription in progress in Tax Enrolments" - {

      "must redirect to the in-progress organisation is enrolled page" in {

        val application = applicationBuilder(journeyData = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          when(mockTaxEnrolmentsService.hasManageIsaSubscriptionInProgress(anyArg[String])(anyArg[HeaderCarrier]))
            .thenReturn(Future.successful(true))

          val authAction = authenticatedIdentifierAction(
            successfulAuthConnector(
              affinityGroup = Some(Organisation),
              groupId = Some(testGroupId),
              credentials = Some(testCredentials),
              credentialRole = Some(testCredentialRoleUser)
            ),
            appConfig,
            bodyParsers
          )

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest(GET, routes.TaskListController.onPageLoad().url))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(
            routes.OrganisationIsEnrolledController.onPageLoad(enrolmentInProgress = true).url
          )
        }
      }

      "must allow sign out without redirecting" in {

        val application = applicationBuilder(journeyData = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = authenticatedIdentifierAction(
            successfulAuthConnector(
              affinityGroup = Some(Organisation),
              groupId = Some(testGroupId),
              credentials = Some(testCredentials),
              credentialRole = Some(testCredentialRoleUser)
            ),
            appConfig,
            bodyParsers
          )

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest(GET, controllers.auth.routes.AuthController.signOut().url))

          status(result) mustBe OK
          verify(mockTaxEnrolmentsService, times(0))
            .hasManageIsaSubscriptionInProgress(anyArg[String])(anyArg[HeaderCarrier])
        }
      }

      "must return internal server error when the Tax Enrolments check fails" in {

        val application = applicationBuilder(journeyData = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          when(mockTaxEnrolmentsService.hasManageIsaSubscriptionInProgress(anyArg[String])(anyArg[HeaderCarrier]))
            .thenReturn(Future.failed(new RuntimeException("Tax Enrolments failed")))

          val authAction = authenticatedIdentifierAction(
            successfulAuthConnector(
              affinityGroup = Some(Organisation),
              groupId = Some(testGroupId),
              credentials = Some(testCredentials),
              credentialRole = Some(testCredentialRoleUser)
            ),
            appConfig,
            bodyParsers
          )

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest(GET, routes.TaskListController.onPageLoad().url))

          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
    }

    "the auth response does not match any expected pattern" - {

      "must redirect to the unauthorised page (default case)" in {

        val application = applicationBuilder(journeyData = None).build()

        running(application) {

          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = authenticatedIdentifierAction(
            successfulAuthConnector(
              groupId = None,
              affinityGroup = None,
              credentials = Some(testCredentials),
              credentialRole = Some(testCredentialRoleUser)
            ),
            appConfig,
            bodyParsers
          )

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
        }
      }
    }
  }
}
