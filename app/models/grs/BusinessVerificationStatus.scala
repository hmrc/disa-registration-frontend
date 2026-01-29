package models.grs

import play.api.libs.json.{Format, JsResult, JsString, JsValue}


sealed trait BusinessVerificationStatus

case object BvPass extends BusinessVerificationStatus

case object BvFail extends BusinessVerificationStatus

case object BvUnchallenged extends BusinessVerificationStatus

case object CtEnrolled extends BusinessVerificationStatus

object BusinessVerificationStatus {
  val BvPassKey = "PASS"
  val BvFailKey = "FAIL"
  val BvUnchallengedKey = "UNCHALLENGED"
  val CtEnrolledKey = "CT_ENROLLED"

  implicit val format: Format[BusinessVerificationStatus] = new Format[BusinessVerificationStatus] {
    override def writes(bvState: BusinessVerificationStatus): JsValue =
      bvState match {
        case BvPass => JsString(BvPassKey)
        case BvFail => JsString(BvFailKey)
        case BvUnchallenged => JsString(BvUnchallengedKey)
        case CtEnrolled => JsString(CtEnrolledKey)
      }

    override def reads(json: JsValue): JsResult[BusinessVerificationStatus] =
      json.validate[String].map {
        case BvPassKey => BvPass
        case BvFailKey => BvFail
        case BvUnchallengedKey => BvUnchallenged
        case CtEnrolledKey => CtEnrolled
      }
  }
}