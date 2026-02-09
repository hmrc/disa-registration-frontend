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

package base

import config.FrontendAppConfig
import connectors.DisaRegistrationConnector
import controllers.actions.{DataRequiredAction, DataRequiredActionImpl, DataRetrievalAction, FakeDataRetrievalAction, FakeIdentifierAction, IdentifierAction}
import handlers.ErrorHandler
import models.journeydata.JourneyData
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.Injector
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.mvc.Results.{BadRequest, InternalServerError}
import play.api.mvc.{PlayBodyParsers, RequestHeader}
import play.api.test.FakeRequest
import play.api.{Application, inject}
import services.{AuditService, GrsService, JourneyAnswersService, SubmissionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.{JourneyDataBuilder, TestData}

import scala.concurrent.{ExecutionContext, Future}

trait SpecBase
    extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneAppPerSuite
    with AuthTestSupport
    with BeforeAndAfterEach
    with TestData
    with MockitoSugar
    with JourneyDataBuilder {

  implicit def messages(implicit app: Application): Messages =
    app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  def messages(key: String)(implicit app: Application): String = messages(app).messages(key)

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  // Mocks
  protected val mockAuditConnector: AuditConnector                       = mock[AuditConnector]
  protected val mockDisaRegistrationConnector: DisaRegistrationConnector = mock[DisaRegistrationConnector]
  protected val mockJourneyAnswersService: JourneyAnswersService         = mock[JourneyAnswersService]
  protected val mockSubmissionService: SubmissionService                 = mock[SubmissionService]
  protected val mockGrsService: GrsService                               = mock[GrsService]
  protected val mockAuditService: AuditService                           = mock[AuditService]
  protected val mockHttpClient: HttpClientV2                             = mock[HttpClientV2]
  protected val mockAppConfig: FrontendAppConfig                         = mock[FrontendAppConfig]
  protected val mockRequestBuilder: RequestBuilder                       = mock[RequestBuilder]
  protected val mockErrorHandler: ErrorHandler                           = mock[ErrorHandler]

  override def beforeEach(): Unit = {
    Mockito.reset(
      mockErrorHandler,
      mockAuditConnector,
      mockDisaRegistrationConnector,
      mockJourneyAnswersService,
      mockSubmissionService,
      mockAuditService,
      mockHttpClient,
      mockAppConfig,
      mockRequestBuilder
    )
    when(mockErrorHandler.internalServerError(any[RequestHeader])).thenReturn(Future.successful(InternalServerError))
    when(mockErrorHandler.badRequest(any[RequestHeader])).thenReturn(Future.successful(BadRequest))
  }

  private val parsers = injector.instanceOf[PlayBodyParsers]

  protected def applicationBuilder(
    journeyData: Option[JourneyData],
    overrides: GuiceableModule*
  ): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        inject.bind[DataRequiredAction].to[DataRequiredActionImpl],
        inject.bind[IdentifierAction].to[FakeIdentifierAction],
        inject.bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(journeyData)),
        inject.bind[JourneyAnswersService].toInstance(mockJourneyAnswersService),
        inject.bind[GrsService].toInstance(mockGrsService),
        inject.bind[ErrorHandler].toInstance(mockErrorHandler)
      )
      .overrides(overrides: _*)

  def injector: Injector = app.injector
}
