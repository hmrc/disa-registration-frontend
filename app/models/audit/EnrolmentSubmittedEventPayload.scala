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

package models.audit

import models.YesNoAnswer
import models.grs.GrsCompanyType
import models.journeydata.certificatesofauthority.{CertificatesOfAuthority, CertificatesOfAuthorityYesNo, FinancialOrganisation}
import models.journeydata.isaproducts.{InnovativeFinancialProduct, IsaProduct, IsaProducts}
import models.journeydata.liaisonofficers.{LiaisonOfficer, LiaisonOfficerCommunication}
import models.journeydata.signatories.Signatory
import models.journeydata.thirdparty.{ThirdParty, ThirdPartyOrganisations}
import models.journeydata.{CorrespondenceAddress, JourneyData, OrganisationDetails}
import play.api.libs.json.{Json, OWrites}

final case class EnrolmentSubmittedEventPayload(
  subscriptionId: Option[String],
  groupType: Option[String],
  isaManagerProviderDetails: Option[IsaManagerProviderDetails],
  orgEmailAddress: Option[String],
  liaisonOfficers: Option[Seq[LiaisonOfficerDetail]],
  signatories: Option[Seq[SignatoryDetail]],
  isaProducts: Option[IsaProductsDetail],
  certificateAuthority: Option[CertificateAuthorityDetail],
  furtherInformation: Option[FurtherInformation]
)

object EnrolmentSubmittedEventPayload {

  def fromJourneyData(journeyData: JourneyData): EnrolmentSubmittedEventPayload =
    EnrolmentSubmittedEventPayload(
      subscriptionId = journeyData.formBundleId,
      groupType = journeyData.businessVerification.flatMap(_.companyType).map(groupType),
      isaManagerProviderDetails = journeyData.organisationDetails.map(IsaManagerProviderDetails.from),
      orgEmailAddress = journeyData.organisationEmail.flatMap(_.organisationEmail),
      liaisonOfficers = journeyData.liaisonOfficers.map(_.liaisonOfficers.map(LiaisonOfficerDetail.from)),
      signatories = journeyData.signatories.map(_.signatories.map(SignatoryDetail.from)),
      isaProducts = journeyData.isaProducts.map(IsaProductsDetail.from),
      certificateAuthority = journeyData.certificatesOfAuthority.map(CertificateAuthorityDetail.from),
      furtherInformation = journeyData.thirdPartyOrganisations.map(FurtherInformation.from)
    )

  // Fixed English labels, so the audit event doesn't change if the page content is reworded.
  private def groupType(companyType: GrsCompanyType): String =
    companyType match {
      case GrsCompanyType.LimitedCompany                 => "Limited company"
      case GrsCompanyType.EuropeanInstitutionWithAUkBase => "European institution with a UK base"
      case GrsCompanyType.IncorporatedFriendlySociety    => "Incorporated friendly society"
      case GrsCompanyType.RegisteredFriendlySociety      => "Registered friendly society"
      case GrsCompanyType.GeneralPartnership             => "General partnership"
      case GrsCompanyType.ScottishPartnership            => "Scottish partnership"
      case GrsCompanyType.ScottishLimitedPartnership     => "Scottish limited partnership"
      case GrsCompanyType.LimitedPartnership             => "Limited partnership"
      case GrsCompanyType.LimitedLiabilityPartnership    => "Limited liability partnership"
    }

  private[audit] def yesNo(answer: YesNoAnswer): String =
    answer match {
      case YesNoAnswer.Yes => "Yes"
      case YesNoAnswer.No  => "No"
    }

  private[audit] def yesNo(answer: Boolean): String =
    if (answer) "Yes" else "No"

  implicit val writes: OWrites[EnrolmentSubmittedEventPayload] = Json.writes[EnrolmentSubmittedEventPayload]
}

final case class IsaManagerProviderDetails(
  existingManager: Option[String],
  zReference: Option[String],
  tradingUsingDifferentName: Option[String],
  tradingName: Option[String],
  fcaNumber: Option[String],
  useRegisteredAddress: Option[String],
  correspondenceAddress: Option[CorrespondenceAddressDetail],
  orgTelephoneNumber: Option[String]
)

object IsaManagerProviderDetails {
  import EnrolmentSubmittedEventPayload.yesNo

  def from(details: OrganisationDetails): IsaManagerProviderDetails =
    IsaManagerProviderDetails(
      existingManager = details.registeredToManageIsa.map(yesNo),
      zReference = details.zRefNumber.filter(_ => details.registeredToManageIsa.contains(YesNoAnswer.Yes)),
      tradingUsingDifferentName = details.tradingUsingDifferentName.map(yesNo),
      tradingName = details.tradingName.filter(_ => details.tradingUsingDifferentName.contains(YesNoAnswer.Yes)),
      fcaNumber = details.fcaNumber,
      useRegisteredAddress = details.registeredAddressCorrespondence.map(yesNo),
      correspondenceAddress = details.correspondenceAddress.map(CorrespondenceAddressDetail.from),
      orgTelephoneNumber = details.orgTelephoneNumber
    )

  implicit val writes: OWrites[IsaManagerProviderDetails] = Json.writes[IsaManagerProviderDetails]
}

final case class CorrespondenceAddressDetail(
  UPRN: Option[String],
  addressLine1: Option[String],
  addressLine2: Option[String],
  addressLine3: Option[String],
  postCode: Option[String]
)

object CorrespondenceAddressDetail {

  def from(address: CorrespondenceAddress): CorrespondenceAddressDetail =
    CorrespondenceAddressDetail(
      UPRN = address.uprn,
      addressLine1 = address.addressLine1,
      addressLine2 = address.addressLine2,
      addressLine3 = address.addressLine3,
      postCode = address.postCode
    )

  implicit val writes: OWrites[CorrespondenceAddressDetail] = Json.writes[CorrespondenceAddressDetail]
}

final case class LiaisonOfficerDetail(
  name: Option[String],
  emailAddress: Option[String],
  telephoneNumber: Option[String],
  communicationPreference: CommunicationPreference
)

object LiaisonOfficerDetail {

  def from(officer: LiaisonOfficer): LiaisonOfficerDetail =
    LiaisonOfficerDetail(
      name = officer.fullName,
      emailAddress = officer.email,
      telephoneNumber = officer.phoneNumber,
      communicationPreference = CommunicationPreference(
        post = officer.communication.contains(LiaisonOfficerCommunication.ByPost),
        email = officer.communication.contains(LiaisonOfficerCommunication.ByEmail),
        telephone = officer.communication.contains(LiaisonOfficerCommunication.ByPhone)
      )
    )

  implicit val writes: OWrites[LiaisonOfficerDetail] = Json.writes[LiaisonOfficerDetail]
}

final case class CommunicationPreference(post: Boolean, email: Boolean, telephone: Boolean)

object CommunicationPreference {
  implicit val writes: OWrites[CommunicationPreference] = Json.writes[CommunicationPreference]
}

final case class SignatoryDetail(name: Option[String], position: Option[String])

object SignatoryDetail {

  def from(signatory: Signatory): SignatoryDetail =
    SignatoryDetail(name = signatory.fullName, position = signatory.jobTitle)

  implicit val writes: OWrites[SignatoryDetail] = Json.writes[SignatoryDetail]
}

final case class IsaProductsDetail(
  cashISA: Boolean,
  cashJuniorISA: Boolean,
  stocksSharesISA: Boolean,
  stocksSharesJuniorISA: Boolean,
  innovativeFinanceISA: Boolean,
  innovativeFinanceISADetails: Option[Seq[InnovativeFinanceIsaDetail]]
)

object IsaProductsDetail {

  def from(products: IsaProducts): IsaProductsDetail = {
    val selected             = products.isaProducts.getOrElse(Seq.empty)
    val innovativeFinanceIsa = selected.contains(IsaProduct.InnovativeFinanceIsas)

    IsaProductsDetail(
      cashISA = selected.contains(IsaProduct.CashIsas),
      cashJuniorISA = selected.contains(IsaProduct.CashJuniorIsas),
      stocksSharesISA = selected.contains(IsaProduct.StocksAndSharesIsas),
      stocksSharesJuniorISA = selected.contains(IsaProduct.StocksAndShareJuniorIsas),
      innovativeFinanceISA = innovativeFinanceIsa,
      innovativeFinanceISADetails = Option.when(innovativeFinanceIsa)(Seq(InnovativeFinanceIsaDetail.from(products)))
    )
  }

  implicit val writes: OWrites[IsaProductsDetail] = Json.writes[IsaProductsDetail]
}

final case class InnovativeFinanceIsaDetail(
  loans36HPermissions: Boolean,
  loansPlatform36HPermissions: Boolean,
  loansPlatform36HPermissionsDetails: Option[Seq[PlatformDetail]],
  crowdfundedDebentures: Boolean,
  ltafRedemption: Boolean
)

object InnovativeFinanceIsaDetail {

  def from(products: IsaProducts): InnovativeFinanceIsaDetail = {
    val selected        = products.innovativeFinancialProducts.getOrElse(Seq.empty)
    val loansOnPlatform = selected.contains(InnovativeFinancialProduct.PeertopeerLoansUsingAPlatformWith36hPermissions)

    InnovativeFinanceIsaDetail(
      loans36HPermissions = selected.contains(InnovativeFinancialProduct.PeertopeerLoansAndHave36hPermissions),
      loansPlatform36HPermissions = loansOnPlatform,
      loansPlatform36HPermissionsDetails = Option.when(loansOnPlatform)(
        Seq(
          PlatformDetail(
            nameOfPlatform = products.p2pPlatform,
            platformFcaNumber = products.p2pPlatformNumber.flatMap(_.trim.toLongOption)
          )
        )
      ),
      crowdfundedDebentures = selected.contains(InnovativeFinancialProduct.CrowdFundedDebentures),
      ltafRedemption = selected.contains(InnovativeFinancialProduct.LongTermAssetFunds)
    )
  }

  implicit val writes: OWrites[InnovativeFinanceIsaDetail] = Json.writes[InnovativeFinanceIsaDetail]
}

final case class PlatformDetail(nameOfPlatform: Option[String], platformFcaNumber: Option[Long])

object PlatformDetail {
  implicit val writes: OWrites[PlatformDetail] = Json.writes[PlatformDetail]
}

final case class CertificateAuthorityDetail(
  certificationDeclared: Option[String],
  articlesApply: Option[Seq[Map[String, Boolean]]],
  typeOfOrganisation: Option[Seq[Map[String, Boolean]]]
)

object CertificateAuthorityDetail {

  def from(certificates: CertificatesOfAuthority): CertificateAuthorityDetail = {
    val declared = certificates.certificatesYesNo

    CertificateAuthorityDetail(
      certificationDeclared = declared.map {
        case CertificatesOfAuthorityYesNo.Yes => "Yes"
        case CertificatesOfAuthorityYesNo.No  => "No"
      },
      articlesApply = certificates.fcaArticles
        .filter(_ => declared.contains(CertificatesOfAuthorityYesNo.Yes))
        .map(_.map(article => Map(article.toString -> true))),
      typeOfOrganisation = certificates.financialOrganisation
        .filter(_ => declared.contains(CertificatesOfAuthorityYesNo.No))
        .map(_.map(organisation => Map(organisationKey(organisation) -> true)))
    )
  }

  private def organisationKey(organisation: FinancialOrganisation): String =
    organisation match {
      case FinancialOrganisation.AssuranceUndertaking => "assuranceUndertakingServices"
      case other                                      => other.toString
    }

  implicit val writes: OWrites[CertificateAuthorityDetail] = Json.writes[CertificateAuthorityDetail]
}

final case class FurtherInformation(
  outsourcedArrangements: Option[String],
  outsourcedArrangementsDetails: Option[Seq[OutsourcedArrangementDetail]],
  connectedParties: Option[String],
  connectedPartiesFirms: Option[Seq[String]]
)

object FurtherInformation {
  import EnrolmentSubmittedEventPayload.yesNo

  def from(organisations: ThirdPartyOrganisations): FurtherInformation = {
    val outsourced = organisations.managedByThirdParty.contains(YesNoAnswer.Yes)
    val connected  = organisations.connectedOrganisations

    FurtherInformation(
      outsourcedArrangements = organisations.managedByThirdParty.map(yesNo),
      outsourcedArrangementsDetails =
        Option.when(outsourced)(organisations.thirdParties.map(OutsourcedArrangementDetail.from)),
      connectedParties = Option.when(outsourced)(yesNo(connected.nonEmpty)),
      connectedPartiesFirms = Option.when(outsourced && connected.nonEmpty)(connected)
    )
  }

  implicit val writes: OWrites[FurtherInformation] = Json.writes[FurtherInformation]
}

final case class OutsourcedArrangementDetail(
  nameOfFirm: Option[String],
  fcaNumber: Option[String],
  returnsReportingOutsourced: Option[String],
  feesCommissionAndIncentives: Option[String],
  percentageCommission: Option[String]
)

object OutsourcedArrangementDetail {
  import EnrolmentSubmittedEventPayload.yesNo

  def from(thirdParty: ThirdParty): OutsourcedArrangementDetail =
    OutsourcedArrangementDetail(
      nameOfFirm = thirdParty.thirdPartyName,
      fcaNumber = thirdParty.thirdPartyFrn,
      returnsReportingOutsourced = thirdParty.managingIsaReturns.map(yesNo),
      feesCommissionAndIncentives = thirdParty.usingInvestorFunds.map(yesNo),
      percentageCommission = thirdParty.investorFundsPercentage
        .filter(_ => thirdParty.usingInvestorFunds.contains(YesNoAnswer.Yes))
        .map(percentage => s"$percentage%")
    )

  implicit val writes: OWrites[OutsourcedArrangementDetail] = Json.writes[OutsourcedArrangementDetail]
}
