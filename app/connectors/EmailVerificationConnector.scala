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

package connectors

import com.typesafe.config.Config
import config.FrontendAppConfig
import models.emailverification.*
import org.apache.pekko.actor.ActorSystem
import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.HttpReads.Implicits.*

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailVerificationConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: FrontendAppConfig,
  override protected val actorSystem: ActorSystem,
  configurationProvider: Configuration
)(implicit ec: ExecutionContext)
    extends BaseConnector
    with Logging {

  override protected val configuration: Config = configurationProvider.underlying

  def sendCode(email: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = s"${appConfig.emailVerificationBaseUrl}/email-verification/v2/send-code"

    retryFor[Unit]("POST Email Verification send code")(retryCondition) {
      httpClient
        .post(url"$url")
        .withBody(Json.toJson(SendEmailVerificationCodeRequest(email)))
        .execute[HttpResponse]
        .map { response =>
          response.status match {
            case OK =>
              ()

            case status =>
              val msg = upstreamErrorMessage(
                endpoint = "POST /v2/send-code",
                status = response.status,
                body = response.body
              )
              logger.error(msg)
              throw UpstreamErrorResponse(
                message = msg,
                statusCode = response.status,
                reportAs = INTERNAL_SERVER_ERROR
              )
          }
        }
    }
  }

  def verifyCode(email: String, code: String)(implicit hc: HeaderCarrier): Future[VerifyEmailCodeResult] = {
    val url = s"${appConfig.emailVerificationBaseUrl}/email-verification/v2/verify-code"

    retryFor[VerifyEmailCodeResult]("POST Email Verification verify code")(retryCondition) {
      httpClient
        .post(url"$url")
        .withBody(Json.toJson(VerifyEmailCodeRequest(email, code)))
        .execute[HttpResponse]
        .map { response =>
          response.status match {
            case OK =>
              VerifyEmailCodeResult.Verified

            case status if status >= BAD_REQUEST && status < INTERNAL_SERVER_ERROR =>
              logger.warn(
                upstreamErrorMessage(
                  endpoint = "POST /v2/verify-code",
                  status = response.status,
                  body = response.body
                )
              )
              VerifyEmailCodeResult.InvalidCode

            case _ =>
              val msg = upstreamErrorMessage(
                endpoint = "POST /v2/verify-code",
                status = response.status,
                body = response.body
              )
              logger.error(msg)
              throw UpstreamErrorResponse(
                message = msg,
                statusCode = response.status,
                reportAs = INTERNAL_SERVER_ERROR
              )
          }
        }
    }
  }

  private def upstreamErrorMessage(endpoint: String, status: Int, body: String): String =
    s"Email verification call [$endpoint] failed with upstream status [$status] and body [$body]"
}
