package models.session

import play.api.libs.json.{Json, OFormat}

import java.time.Instant

case class Session(groupId: String, continuationAuditEventSent: Boolean, lastUpdated: Instant)

object Session {
  implicit val format: OFormat[Session] = Json.format[Session]
}
