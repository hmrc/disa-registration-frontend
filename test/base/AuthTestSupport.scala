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

import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.*
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait AuthTestSupport {

  protected def successfulAuthConnector(
                                         groupId: Option[String] = Some("group-id"),
                                         affinityGroup: Option[AffinityGroup] = Some(AffinityGroup.Organisation)
                                       ): AuthConnector =
    new AuthConnector {
      
      override def authorise[A](
                                 predicate: Predicate,
                                 retrieval: Retrieval[A]
                               )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {

        val result: Option[String] ~ Option[AffinityGroup] =
          new ~(groupId, affinityGroup)

        Future.successful(result.asInstanceOf[A])
      }
    }

  protected def failingAuthConnector(
                                      exception: Throwable
                                    ): AuthConnector =
    new AuthConnector {
      
      override def authorise[A](
                                 predicate: Predicate,
                                 retrieval: Retrieval[A]
                               )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
        Future.failed(exception)
    }
}
