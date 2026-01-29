package models.grs

import play.api.libs.json.{Format, JsResult, JsString, JsValue}

sealed trait BusinessRegistrationStatus

case object RegisteredStatus extends BusinessRegistrationStatus

case object FailedStatus extends BusinessRegistrationStatus

case object NotCalledStatus extends BusinessRegistrationStatus

object BusinessRegistrationStatus {
  implicit val format: Format[BusinessRegistrationStatus] = new Format[BusinessRegistrationStatus] {
    val RegisteredKey = "REGISTERED"
    val FailedKey = "REGISTRATION_FAILED"
    val NotCalledKey = "REGISTRATION_NOT_CALLED"

    override def writes(status: BusinessRegistrationStatus): JsValue =
      status match {
        case RegisteredStatus => JsString(RegisteredKey)
        case FailedStatus => JsString(FailedKey)
        case NotCalledStatus => JsString(NotCalledKey)
      }

    override def reads(json: JsValue): JsResult[BusinessRegistrationStatus] =
      json.validate[String].map {
        case RegisteredKey => RegisteredStatus
        case FailedKey => FailedStatus
        case NotCalledKey => NotCalledStatus
      }
  }
}