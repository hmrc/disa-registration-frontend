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

import com.google.inject.{Inject, Singleton}
import models.grs.{GrsCompanyType, GrsIdentificationService}
import play.api.Configuration
import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URLEncoder

@Singleton
class FrontendAppConfig @Inject(config: Configuration) extends ServicesConfig(config) {

  lazy val host: String    = getString("host")
  lazy val appName: String = getString("appName")
  val selfBaseUrl: String  = baseUrl(appName)

  private lazy val contactHost                  = getString("contact-frontend.host")
  private lazy val contactFormServiceIdentifier = "disa-registration-frontend"

  def feedbackUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${host + request.uri}&useServiceNavigation"

  val loginUrl: String              = getString("urls.login")
  val loginContinueUrl: String      = getString("urls.loginContinue")
  val signOutUrl: String            = getString("urls.signOut")
  val isaManagerGuidanceUrl: String = getString("urls.isaManagerGuidance")
  val ggSignInUrl: String           = s"$loginUrl?continue=${URLEncoder.encode(loginContinueUrl, "UTF-8")}"

  lazy val disaRegistrationBaseUrl: String  = baseUrl("disa-registration")
  lazy val taxEnrolmentsBaseUrl: String     = baseUrl("tax-enrolments")
  lazy val addressLookupBaseUrl: String     = baseUrl("address-lookup")
  lazy val emailVerificationBaseUrl: String = baseUrl("email-verification")

  private lazy val exitSurveyBaseUrl: String = baseUrl("feedback-frontend")

  lazy val exitSurveyUrl: String = s"$exitSurveyBaseUrl/feedback/disa-registration-frontend?useServiceNavigation"

  lazy val languageTranslationEnabled: Boolean =
    getBoolean("features.welsh-translation")

  lazy val europeanInstitutionWithAUkBaseEnabled: Boolean =
    getBoolean("features.grs.european-institution-with-a-uk-base-enabled")
  lazy val generalPartnershipEnabled: Boolean             =
    getBoolean("features.grs.general-partnership-enabled")
  lazy val incorporatedFriendlySocietyEnabled: Boolean    =
    getBoolean("features.grs.incorporated-friendly-society-enabled")
  lazy val limitedCompanyEnabled: Boolean                 =
    getBoolean("features.grs.limited-company-enabled")
  lazy val limitedLiabilityPartnershipEnabled: Boolean    =
    getBoolean("features.grs.limited-liability-partnership-enabled")
  lazy val limitedPartnershipEnabled: Boolean             =
    getBoolean("features.grs.limited-partnership-enabled")
  lazy val registeredFriendlySocietyEnabled: Boolean      =
    getBoolean("features.grs.registered-friendly-society-enabled")
  lazy val scottishLimitedPartnershipEnabled: Boolean     =
    getBoolean("features.grs.scottish-limited-partnership-enabled")
  lazy val scottishPartnershipEnabled: Boolean            =
    getBoolean("features.grs.scottish-partnership-enabled")

  lazy val timeout: Int       = getInt("timeout-dialog.timeout")
  lazy val countdown: Int     = getInt("timeout-dialog.countdown")
  lazy val cacheTtl: Long     = getInt("mongodb.timeToLiveInSeconds")
  lazy val bvLockoutTtl: Long = getInt("bvLockout.timeToLiveInMinutes")

  lazy val listOfRegisteredIsaManagersUrl: String = getString("urls.external.listOfRegisteredIsaManagers")
  lazy val p2pLoansInformationUrl: String         = getString("urls.external.p2pLoansInformation")
  lazy val businessTaxAccountUrl: String          = getString("urls.external.businessTaxAccount")
  lazy val isaManagersGuidanceUrl: String         = getString("urls.external.isaManagerGuidanceCollection")

  lazy val incorporatedEntityIdentificationHost: String =
    baseUrl("incorporated-entity-identification-frontend")

  lazy val partnershipIdentificationHost: String =
    baseUrl("partnership-identification-frontend")

  private def grsHost(identificationService: GrsIdentificationService): String =
    identificationService match {
      case GrsIdentificationService.IncorporatedEntityIdentification => incorporatedEntityIdentificationHost
      case GrsIdentificationService.PartnershipIdentification        => partnershipIdentificationHost
    }

  def grsCreateJourneyUrl(companyType: GrsCompanyType): String =
    s"${grsHost(companyType.identificationService)}/${companyType.identificationService.apiBasePath}/${companyType.createJourneyPath}"

  def grsRetrieveResultUrl(companyType: GrsCompanyType, journeyId: String): String =
    s"${grsHost(companyType.identificationService)}/${companyType.identificationService.apiBasePath}/journey/$journeyId"

  lazy val grsCallback: String = "/obligations/enrolment/isa/incorporated-identity-callback"

  lazy val accessibilityStatementUrl = "/accessibility-statement/disa-registration-frontend?useServiceNavigation"

  lazy val manageIsaEnrolmentKey: String = getString("enrolments.manageIsa")

  lazy val maxLiaisonOfficers: Int = getInt("maxLiaisonOfficers")
  lazy val maxSignatories: Int     = getInt("max-signatories")
  lazy val maxThirdParties: Int    = getInt("max-third-parties")
}
