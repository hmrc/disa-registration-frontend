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

package config

import base.SpecBase
import play.api.Configuration
import play.api.i18n.Lang
import play.api.test.FakeRequest
import uk.gov.hmrc.hmrcfrontend.config.SupportedLanguagesConfig.{cy, en}

import java.net.URLEncoder

class FrontendAppConfigSpec extends SpecBase {

  private val frontendHost = "http://localhost:1200"

  private val loginUrl         = "http://localhost:9949/auth-login-stub/gg-sign-in"
  private val loginContinueUrl = "http://localhost:1200/register-your-individual-savings-account"
  private val signOutUrl       = "http://localhost:9553/bas-gateway/sign-out-without-state"

  private val isaManagerGuidanceUrl = "https://www.gov.uk/guidance/manage-isa-subscriptions-for-your-investors"

  private val listOfRegisteredIsaManagersUrl =
    "https://www.gov.uk/government/publications/list-of-individual-savings-account-isa-managers-approved-by-hmrc/registered-individual-savings-account-isa-managers"

  private val p2pLoansInformationUrl =
    "https://www.gov.uk/government/consultations/isa-qualifying-investments-consultation-on-including-peer-to-peer-loans/isa-qualifying-investments-consultation-on-including-peer-to-peer-loans#:~:text=Peer%2Dto%2Dpeer%20loans%20are,terms%20agreed%20between%20the%20parties."

  private val isaManagersGuidanceCollectionUrl =
    "https://www.gov.uk/government/collections/isa-managers-guidance"

  private val businessTaxAccountUrl = "https://businesstaxaccount.co.uk"
  private val manageIsaEnrolmentKey = "HMRC-DISA-ORG"

  private val configuration: Configuration = Configuration.from(
    Map(
      "host"                                                                       -> frontendHost,
      "appName"                                                                    -> "disa-registration-frontend",
      "contact-frontend.host"                                                      -> "http://localhost:9250",
      "urls.login"                                                                 -> loginUrl,
      "urls.loginContinue"                                                         -> loginContinueUrl,
      "urls.signOut"                                                               -> signOutUrl,
      "urls.isaManagerGuidance"                                                    -> isaManagerGuidanceUrl,
      "urls.external.listOfRegisteredIsaManagers"                                  -> listOfRegisteredIsaManagersUrl,
      "urls.external.p2pLoansInformation"                                          -> p2pLoansInformationUrl,
      "urls.external.isaManagerGuidanceCollection"                                 -> isaManagersGuidanceCollectionUrl,
      "urls.external.businessTaxAccount"                                           -> businessTaxAccountUrl,
      "enrolments.manageIsa"                                                       -> manageIsaEnrolmentKey,
      "features.welsh-translation"                                                 -> true,
      "timeout-dialog.timeout"                                                     -> 900,
      "timeout-dialog.countdown"                                                   -> 120,
      "mongodb.timeToLiveInSeconds"                                                -> 900,
      "bvLockout.timeToLiveInMinutes"                                              -> 15,
      "maxLiaisonOfficers"                                                         -> 5,
      "max-signatories"                                                            -> 6,
      "max-third-parties"                                                          -> 7,
      "microservice.services.disa-registration-frontend.protocol"                  -> "http",
      "microservice.services.disa-registration-frontend.host"                      -> "localhost",
      "microservice.services.disa-registration-frontend.port"                      -> 1200,
      "microservice.services.disa-registration.protocol"                           -> "http",
      "microservice.services.disa-registration.host"                               -> "localhost",
      "microservice.services.disa-registration.port"                               -> 1201,
      "microservice.services.tax-enrolments.protocol"                              -> "http",
      "microservice.services.tax-enrolments.host"                                  -> "localhost",
      "microservice.services.tax-enrolments.port"                                  -> 1204,
      "microservice.services.address-lookup.protocol"                              -> "http",
      "microservice.services.address-lookup.host"                                  -> "localhost",
      "microservice.services.address-lookup.port"                                  -> 1202,
      "microservice.services.email-verification.protocol"                          -> "http",
      "microservice.services.email-verification.host"                              -> "localhost",
      "microservice.services.email-verification.port"                              -> 1203,
      "microservice.services.feedback-frontend.protocol"                           -> "http",
      "microservice.services.feedback-frontend.host"                               -> "localhost",
      "microservice.services.feedback-frontend.port"                               -> 9514,
      "microservice.services.incorporated-entity-identification-frontend.protocol" -> "http",
      "microservice.services.incorporated-entity-identification-frontend.host"     -> "localhost",
      "microservice.services.incorporated-entity-identification-frontend.port"     -> 9754
    )
  )

  private val appConfig = new FrontendAppConfig(configuration)

  "FrontendAppConfig" - {

    "must return host" in {
      appConfig.host mustBe frontendHost
    }

    "must return appName" in {
      appConfig.appName mustBe "disa-registration-frontend"
    }

    "must generate selfBaseUrl" in {
      appConfig.selfBaseUrl mustBe "http://localhost:1200"
    }

    "must generate feedbackUrl" in {
      implicit val request = FakeRequest("GET", "/some-page?foo=bar")

      appConfig.feedbackUrl mustBe
        "http://localhost:9250/contact/beta-feedback?service=disa-registration-frontend&backUrl=http://localhost:1200/some-page?foo=bar"
    }

    "must return loginUrl" in {
      appConfig.loginUrl mustBe loginUrl
    }

    "must return loginContinueUrl" in {
      appConfig.loginContinueUrl mustBe loginContinueUrl
    }

    "must return signOutUrl" in {
      appConfig.signOutUrl mustBe signOutUrl
    }

    "must return isaManagerGuidanceUrl" in {
      appConfig.isaManagerGuidanceUrl mustBe isaManagerGuidanceUrl
    }

    "must generate ggSignInUrl" in {
      val encodedContinueUrl = URLEncoder.encode(loginContinueUrl, "UTF-8")

      appConfig.ggSignInUrl mustBe s"$loginUrl?continue=$encodedContinueUrl"
    }

    "must generate DISA backend URL" in {
      appConfig.disaRegistrationBaseUrl mustBe "http://localhost:1201"
    }

    "must generate Tax Enrolments URL" in {
      appConfig.taxEnrolmentsBaseUrl mustBe "http://localhost:1204"
    }

    "must generate address lookup URL" in {
      appConfig.addressLookupBaseUrl mustBe "http://localhost:1202"
    }

    "must generate email verification URL" in {
      appConfig.emailVerificationBaseUrl mustBe "http://localhost:1203"
    }

    "must generate exit survey URL" in {
      appConfig.exitSurveyUrl mustBe "http://localhost:9514/feedback/disa-registration-frontend"
    }

    "must return whether language translation is enabled" in {
      appConfig.languageTranslationEnabled mustBe true
    }

    "must return language Map" in {
      appConfig.languageMap mustBe Map("en" -> Lang(en), "cy" -> Lang(cy))
    }

    "must return timeout value" in {
      appConfig.timeout mustBe 900
    }

    "must return countdown value" in {
      appConfig.countdown mustBe 120
    }

    "must return cacheTtl value" in {
      appConfig.cacheTtl mustBe 900
    }

    "must return bvLockoutTtl value" in {
      appConfig.bvLockoutTtl mustBe 15
    }

    "must return listOfRegisteredIsaManagersUrl" in {
      appConfig.listOfRegisteredIsaManagersUrl mustBe listOfRegisteredIsaManagersUrl
    }

    "must return p2pLoansInformationUrl" in {
      appConfig.p2pLoansInformationUrl mustBe p2pLoansInformationUrl
    }

    "must return businessTaxAccountUrl" in {
      appConfig.businessTaxAccountUrl mustBe businessTaxAccountUrl
    }

    "must return isaManagersGuidanceUrl" in {
      appConfig.isaManagersGuidanceUrl mustBe isaManagersGuidanceCollectionUrl
    }

    "must return manageIsaEnrolmentKey" in {
      appConfig.manageIsaEnrolmentKey mustBe manageIsaEnrolmentKey
    }

    "must generate incorporatedEntityIdentificationHost" in {
      appConfig.incorporatedEntityIdentificationHost mustBe "http://localhost:9754"
    }

    "must generate GRS retrieve result URL" in {
      val journeyId = "test-journey-id"

      appConfig.grsRetrieveResultUrl(journeyId) mustBe
        "http://localhost:9754/incorporated-entity-identification/api/journey/test-journey-id"
    }

    "must return GRS callback URL" in {
      appConfig.grsCallback mustBe "/obligations/enrolment/isa/incorporated-identity-callback"
    }

    "must return accessibility statement URL" in {
      appConfig.accessibilityStatementUrl mustBe "/accessibility-statement/disa-registration-frontend"
    }

    "must return max liaison officers" in {
      appConfig.maxLiaisonOfficers mustBe 5
    }

    "must return max signatories" in {
      appConfig.maxSignatories mustBe 6
    }

    "must return max third parties" in {
      appConfig.maxThirdParties mustBe 7
    }
  }
}
