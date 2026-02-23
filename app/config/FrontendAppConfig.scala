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
import play.api.Configuration
import play.api.i18n.Lang
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
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${host + request.uri}"

  val loginUrl: String              = getString("urls.login")
  val loginContinueUrl: String      = getString("urls.loginContinue")
  val signOutUrl: String            = getString("urls.signOut")
  val isaManagerGuidanceUrl: String = getString("urls.isaManagerGuidance")
  val ggSignInUrl: String           = s"$loginUrl?continue=${URLEncoder.encode(loginContinueUrl, "UTF-8")}"

  lazy val disaRegistrationBaseUrl: String = baseUrl("disa-registration")

  private lazy val exitSurveyBaseUrl: String = baseUrl("feedback-frontend")

  lazy val exitSurveyUrl: String = s"$exitSurveyBaseUrl/feedback/disa-registration-frontend"

  lazy val languageTranslationEnabled: Boolean =
    getBoolean("features.welsh-translation")

  def languageMap: Map[String, Lang] = Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy")
  )

  lazy val timeout: Int   = getInt("timeout-dialog.timeout")
  lazy val countdown: Int = getInt("timeout-dialog.countdown")
  lazy val cacheTtl: Long = getInt("mongodb.timeToLiveInSeconds")

  lazy val listOfRegisteredIsaManagersUrl: String = getString("urls.external.listOfRegisteredIsaManagers")
  lazy val p2pLoansInformationUrl: String         = getString("urls.external.p2pLoansInformation")

  lazy val incorporatedEntityIdentificationHost: String =
    baseUrl("incorporated-entity-identification-frontend")

  def grsRetrieveResultUrl(journeyId: String): String =
    s"$incorporatedEntityIdentificationHost/incorporated-entity-identification/api/journey/$journeyId"

  lazy val grsCallback: String = s"$host/obligations/enrolment/isa/incorporated-identity-callback"

  lazy val accessibilityStatementUrl = s"/accessibility-statement/disa-registration-frontend"

}
