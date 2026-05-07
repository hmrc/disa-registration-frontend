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

package models

import play.api.mvc.QueryStringBindable

sealed trait ReturnTo

object ReturnTo {

  case object FinalCya extends ReturnTo

  implicit val queryStringBindable: QueryStringBindable[ReturnTo] =
    new QueryStringBindable[ReturnTo] {

      override def bind(
        key: String,
        params: Map[String, Seq[String]]
      ): Option[Either[String, ReturnTo]] =
        params.get(key).flatMap(_.headOption).map {
          case "FinalCya" => Right(FinalCya)
          case other      => Left(s"Unknown returnTo value: $other")
        }

      override def unbind(key: String, value: ReturnTo): String =
        value match {
          case FinalCya => s"$key=FinalCya"
        }
    }
}
