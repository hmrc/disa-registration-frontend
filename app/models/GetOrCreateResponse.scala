package models

import models.journeydata.JourneyData
import play.api.libs.json.{Json, Reads}

case class GetOrCreateResponse(isNewEnrolment: Boolean, journeyData: JourneyData)

object GetOrCreateResponse {
  implicit val reads: Reads[GetOrCreateResponse] = Json.reads[GetOrCreateResponse]
}
