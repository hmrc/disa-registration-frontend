package models

import models.grs.BusinessVerificationLockout
import play.api.libs.json.{Format, JsValue, Json, OFormat}
import utils.JsonFormatSpec

import java.time.Instant

class BusinessVerificationLockoutSpec extends JsonFormatSpec[BusinessVerificationLockout] {

  private val testCreatedAt: Instant = Instant.parse("2026-01-01T00:00:00Z")

  override val model: BusinessVerificationLockout =
    BusinessVerificationLockout(
      utr = testString,
      groupId = Seq(testGroupId),
      createdAt = testCreatedAt
    )

  override val expectedJsonFromWrites: JsValue =
    Json.obj(
      "utr" -> testString,
      "groupId" -> Seq(testGroupId),
      "createdAt" -> Json.toJson(testCreatedAt)(BusinessVerificationLockout.instantFormat)
    )

  override implicit val format: Format[BusinessVerificationLockout] =
    BusinessVerificationLockout.format
}