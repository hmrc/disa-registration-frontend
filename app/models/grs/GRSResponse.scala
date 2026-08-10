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

package models.grs

import models.journeydata.RegisteredAddress
import play.api.libs.json.*

import java.time.LocalDate

sealed trait GRSResponse {
  def identifiersMatch: Boolean
  def businessRegistrationStatus: BusinessRegistrationStatus
  def businessVerificationStatus: Option[BusinessVerificationStatus]
  def bpSafeId: Option[String]
  def companyName: Option[String]
  def companyNumber: Option[String]
  def registeredAddress: Option[RegisteredAddress]
  def utr: Option[String]
}

case class IncorporatedEntityGRSResponse(
  companyNumber: Option[String],
  companyName: Option[String],
  ctutr: Option[String] = None,
  dateOfIncorporation: Option[LocalDate],
  identifiersMatch: Boolean,
  businessRegistrationStatus: BusinessRegistrationStatus,
  businessVerificationStatus: Option[BusinessVerificationStatus],
  bpSafeId: Option[String],
  registeredAddress: Option[RegisteredAddress]
) extends GRSResponse {
  override def utr: Option[String] = ctutr
}

object IncorporatedEntityGRSResponse {

  implicit val writes: OWrites[IncorporatedEntityGRSResponse] = Json.writes[IncorporatedEntityGRSResponse]

  implicit val reads: Reads[IncorporatedEntityGRSResponse] = for {
    companyNumber              <- (JsPath \ "companyProfile" \ "companyNumber").readNullable[String]
    companyName                <- (JsPath \ "companyProfile" \ "companyName").readNullable[String]
    dateOfIncorporation        <- (JsPath \ "companyProfile" \ "dateOfIncorporation").readNullable[LocalDate]
    identifiersMatch           <- (JsPath \ "identifiersMatch").read[Boolean]
    businessRegistrationStatus <-
      (JsPath \ "registration" \ "registrationStatus").read[BusinessRegistrationStatus]
    bpSafeId                   <-
      (JsPath \ "registration" \ "registeredBusinessPartnerId").readNullable[String]
    ctutr                      <- (JsPath \ "ctutr").readNullable[String]
    businessVerificationStatus <-
      (JsPath \ "businessVerification" \ "verificationStatus").readNullable[BusinessVerificationStatus]
    registeredAddress          <-
      (JsPath \ "companyProfile" \ "unsanitisedCHROAddress").readNullable(RegisteredAddress.grsReads)
  } yield IncorporatedEntityGRSResponse(
    companyNumber = companyNumber,
    companyName = companyName,
    ctutr = ctutr,
    dateOfIncorporation = dateOfIncorporation,
    identifiersMatch = identifiersMatch,
    businessRegistrationStatus = businessRegistrationStatus,
    businessVerificationStatus = businessVerificationStatus,
    bpSafeId = bpSafeId,
    registeredAddress = registeredAddress
  )
}

case class PartnershipGRSResponse(
  sautr: Option[String] = None,
  saPostcode: Option[String] = None,
  companyNumber: Option[String] = None,
  companyName: Option[String] = None,
  identifiersMatch: Boolean,
  businessRegistrationStatus: BusinessRegistrationStatus,
  businessVerificationStatus: Option[BusinessVerificationStatus],
  bpSafeId: Option[String],
  registeredAddress: Option[RegisteredAddress]
) extends GRSResponse {
  override def utr: Option[String] = sautr
}

object PartnershipGRSResponse {

  implicit val writes: OWrites[PartnershipGRSResponse] = Json.writes[PartnershipGRSResponse]

  implicit val reads: Reads[PartnershipGRSResponse] = for {
    sautr                      <- (JsPath \ "sautr").readNullable[String]
    saPostcode                 <- (JsPath \ "postcode").readNullable[String]
    companyNumber              <- (JsPath \ "companyProfile" \ "companyNumber").readNullable[String]
    companyName                <- (JsPath \ "companyProfile" \ "companyName").readNullable[String]
    identifiersMatch           <- (JsPath \ "identifiersMatch").read[Boolean]
    businessRegistrationStatus <-
      (JsPath \ "registration" \ "registrationStatus").read[BusinessRegistrationStatus]
    bpSafeId                   <-
      (JsPath \ "registration" \ "registeredBusinessPartnerId").readNullable[String]
    businessVerificationStatus <-
      (JsPath \ "businessVerification" \ "verificationStatus").readNullable[BusinessVerificationStatus]
    registeredAddress          <-
      (JsPath \ "companyProfile" \ "unsanitisedCHROAddress").readNullable(RegisteredAddress.grsReads)
  } yield PartnershipGRSResponse(
    sautr = sautr,
    saPostcode = saPostcode,
    companyNumber = companyNumber,
    companyName = companyName,
    identifiersMatch = identifiersMatch,
    businessRegistrationStatus = businessRegistrationStatus,
    businessVerificationStatus = businessVerificationStatus,
    bpSafeId = bpSafeId,
    registeredAddress = registeredAddress
  )
}
