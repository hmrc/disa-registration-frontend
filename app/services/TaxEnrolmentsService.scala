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

import config.FrontendAppConfig
import connectors.TaxEnrolmentsConnector
import models.taxenrolments.TaxEnrolmentSubscription
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxEnrolmentsService @Inject() (
  connector: TaxEnrolmentsConnector,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends Logging {

  private val redirectStates = Set("PENDING", "SUCCEEDED")

  def hasManageIsaSubscriptionInProgress(groupId: String)(implicit hc: HeaderCarrier): Future[Boolean] =
    connector
      .getSubscriptionsByGroupId(groupId)
      .map { subscriptions =>
        val manageIsaSubscriptions = subscriptions.filter(isManageIsaSubscription(groupId))

        if (manageIsaSubscriptions.isEmpty) {
          false
        } else if (
          manageIsaSubscriptions.exists(subscription => redirectStates.contains(normaliseState(subscription.state)))
        ) {
          true
        } else {
          val states =
            manageIsaSubscriptions.map(subscription => normaliseState(subscription.state)).distinct.mkString(", ")
          logger.error(
            s"Tax Enrolments returned no successful Manage ISA subscriptions for groupId [$groupId]. States found: [$states]"
          )
          throw new IllegalStateException(
            s"Tax Enrolments returned no successful Manage ISA subscriptions. States found: [$states]"
          )
        }
      }

  private def isManageIsaSubscription(groupId: String)(subscription: TaxEnrolmentSubscription): Boolean =
    subscription.serviceName == appConfig.manageIsaEnrolmentKey &&
      subscription.groupIdentifier.contains(groupId)

  private def normaliseState(state: String): String =
    state.trim.toUpperCase
}
