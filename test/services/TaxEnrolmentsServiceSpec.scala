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

package services

import base.SpecBase
import models.taxenrolments.TaxEnrolmentSubscription
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class TaxEnrolmentsServiceSpec extends SpecBase {

  private val manageIsaServiceName = "HMRC-DISA-ORG"

  private def service: TaxEnrolmentsService = {
    when(mockAppConfig.manageIsaEnrolmentKey).thenReturn(manageIsaServiceName)
    new TaxEnrolmentsService(mockTaxEnrolmentsConnector, mockAppConfig)
  }

  private def subscription(
    state: String,
    serviceName: String = manageIsaServiceName,
    groupIdentifier: Option[String] = Some(testGroupId)
  ): TaxEnrolmentSubscription =
    TaxEnrolmentSubscription(
      serviceName = serviceName,
      state = state,
      groupIdentifier = groupIdentifier
    )

  private def connectorReturns(subscriptions: Seq[TaxEnrolmentSubscription]): Unit =
    when(mockTaxEnrolmentsConnector.getSubscriptionsByGroupId(eqTo(testGroupId))(any[HeaderCarrier]))
      .thenReturn(Future.successful(subscriptions))

  "TaxEnrolmentsService" - {

    "hasManageIsaSubscriptionInProgress" - {

      Seq("PENDING", "SUCCEEDED").foreach { state =>
        s"must return true when Tax Enrolments has a $state Manage ISA subscription for the group" in {
          connectorReturns(Seq(subscription(state)))

          service.hasManageIsaSubscriptionInProgress(testGroupId).futureValue mustBe true
        }
      }

      "must return false when Tax Enrolments has no subscriptions for the group" in {
        connectorReturns(Seq.empty)

        service.hasManageIsaSubscriptionInProgress(testGroupId).futureValue mustBe false
      }

      "must return false when Tax Enrolments has subscriptions for a different service" in {
        connectorReturns(Seq(subscription(state = "PENDING", serviceName = "HMRC-OTHER-SERVICE")))

        service.hasManageIsaSubscriptionInProgress(testGroupId).futureValue mustBe false
      }

      "must return false when Tax Enrolments has subscriptions for a different group" in {
        connectorReturns(Seq(subscription(state = "PENDING", groupIdentifier = Some("different-group-id"))))

        service.hasManageIsaSubscriptionInProgress(testGroupId).futureValue mustBe false
      }

      "must return true when at least one matching Manage ISA subscription is successful" in {
        connectorReturns(
          Seq(
            subscription(state = "ERROR", serviceName = "HMRC-OTHER-SERVICE"),
            subscription(state = "PENDING")
          )
        )

        service.hasManageIsaSubscriptionInProgress(testGroupId).futureValue mustBe true
      }

      Seq("ERROR", "OFFLINE", "UNKNOWN").foreach { state =>
        s"must fail when Tax Enrolments has a $state Manage ISA subscription for the group" in {
          connectorReturns(Seq(subscription(state)))

          service.hasManageIsaSubscriptionInProgress(testGroupId).failed.futureValue mustBe a[IllegalStateException]
        }
      }
    }
  }
}
