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

import play.api.libs.json.*

trait ErrorResponse {
  def code: String
  def message: String
}

case class BadRequestErr(message: String) extends ErrorResponse {
  val code = "BAD_REQUEST"
}

case object UnauthorisedErr extends ErrorResponse {
  val code    = "UNAUTHORISED"
  val message = "Unauthorised"
}

case class InternalServerErr(
  message: String = "There has been an issue processing your request"
) extends ErrorResponse {
  val code = "INTERNAL_SERVER_ERROR"
}

object ErrorResponse {
  implicit val badRequestFmt: OFormat[BadRequestErr] = Json.format[BadRequestErr]

  implicit val unauthReads: Reads[UnauthorisedErr.type] = (json: JsValue) =>
    (json \ "code").validate[String].flatMap {
      case UnauthorisedErr.code => JsSuccess(UnauthorisedErr)
      case other                => JsError(s"Unexpected code for UnauthorisedErr: $other")
    }

  implicit val internalErrFmt: OFormat[InternalServerErr] = Json.format[InternalServerErr]

  implicit val errorResponseFormat: OFormat[ErrorResponse] = new OFormat[ErrorResponse] {

    override def writes(error: ErrorResponse): JsObject = error match {
      case error: ErrorResponse => Json.obj("code" -> error.code, "message" -> error.message)
    }

    override def reads(json: JsValue): JsResult[ErrorResponse] =
      (json \ "code").validate[String].flatMap {
        case "BAD_REQUEST"           => badRequestFmt.reads(json)
        case "UNAUTHORISED"          => unauthReads.reads(json)
        case "INTERNAL_SERVER_ERROR" => internalErrFmt.reads(json)
        case other                   => JsError(s"Unknown error code: $other")
      }
  }
}
