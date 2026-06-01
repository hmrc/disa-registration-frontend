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

package utils

import generators.Generators
import models.YesNoAnswer
import models.journeydata.{BusinessVerification, CorrespondenceAddress, JourneyData, OrganisationDetails, OrganisationEmail, RegisteredAddress}
import models.journeydata.certificatesofauthority.CertificatesOfAuthority
import models.journeydata.certificatesofauthority.CertificatesOfAuthorityYesNo.{No, Yes}
import models.journeydata.certificatesofauthority.FcaArticles.Article14
import models.journeydata.certificatesofauthority.FinancialOrganisation.EuropeanInstitution
import models.journeydata.isaproducts.IsaProduct.CashIsas
import models.journeydata.isaproducts.{InnovativeFinancialProduct, IsaProduct, IsaProducts}
import models.journeydata.liaisonofficers.LiaisonOfficerCommunication.ByEmail
import models.journeydata.liaisonofficers.{LiaisonOfficer, LiaisonOfficers}
import models.journeydata.signatories.{Signatories, Signatory}
import models.journeydata.thirdparty.{ThirdParty, ThirdPartyOrganisations}
import uk.gov.hmrc.auth.core.User
import uk.gov.hmrc.auth.core.retrieve.Credentials

import scala.util.Random

trait TestData extends Generators {
  val testString               = "test"
  val testGroupId              = "3147318d-1cd9-4534-a4e8-ae268ea923ed"
  val testEnrolmentId          = "2b2825af-d5a6-4518-a6cb-67ddb4e66952"
  val testZRef                 = s"Z${(1 to 4).map(_ => Random.nextInt(10)).mkString}"
  val testCredentials          = Credentials(testString, testString)
  val testCredentialRoleUser   = User
  val testFormBundleId: String =
    Random.between(100000000000L, 999999999999L).toString

  def emptyJourneyData: JourneyData = JourneyData(testGroupId, testString)

  val testIsaProductsAnswers: IsaProducts =
    IsaProducts(
      isaProducts = Some(IsaProduct.values),
      p2pPlatform = Some("Test Platform"),
      p2pPlatformNumber = Some("1234567"),
      innovativeFinancialProducts = Some(InnovativeFinancialProduct.values)
    )

  val testCoaAnswersWithFinancialOrg: CertificatesOfAuthority =
    CertificatesOfAuthority(
      certificatesYesNo = Some(No),
      financialOrganisation = Some(Seq(EuropeanInstitution))
    )

  val testOrganisationDetails: OrganisationDetails =
    OrganisationDetails(registeredToManageIsa = Some(YesNoAnswer.Yes))

  val testCoaAnswersWithArticles: CertificatesOfAuthority =
    CertificatesOfAuthority(
      certificatesYesNo = Some(Yes),
      fcaArticles = Some(Seq(Article14))
    )

  val testLiaisonOfficers = LiaisonOfficers(Seq(LiaisonOfficer(id = testString, fullName = Some(testString))))

  val testSignatories = Signatories(Seq(Signatory(id = testString, fullName = Some(testString))))

  def emptyJourneyDataWithBusinessVerification: JourneyData =
    emptyJourneyData.copy(businessVerification = Some(testBV))

  def emptyJourneyDataWithFailedBusinessVerification: JourneyData =
    emptyJourneyDataWithBusinessVerification.copy(
      businessVerification = Some(testBV.copy(businessVerificationPassed = Some(false)))
    )

  val completeTaskListOrganisationDetails: OrganisationDetails =
    OrganisationDetails(
      registeredToManageIsa = Some(YesNoAnswer.No),
      tradingUsingDifferentName = Some(YesNoAnswer.No),
      fcaNumber = Some("123456"),
      registeredAddressCorrespondence = Some(YesNoAnswer.Yes),
      orgTelephoneNumber = Some("01234567890")
    )

  val inProgressTaskListOrganisationDetails: OrganisationDetails =
    OrganisationDetails(registeredToManageIsa = Some(YesNoAnswer.Yes))

  val completeTaskListOrganisationEmail: OrganisationEmail =
    OrganisationEmail(Some("test@example.com"), Some(true))

  val unverifiedTaskListOrganisationEmail: OrganisationEmail =
    OrganisationEmail(Some("test@example.com"), Some(false))

  val completeTaskListIsaProducts: IsaProducts =
    IsaProducts(isaProducts = Some(Seq(CashIsas)))

  val completeTaskListCertificatesOfAuthority: CertificatesOfAuthority =
    CertificatesOfAuthority(certificatesYesNo = Some(Yes), fcaArticles = Some(Seq(Article14)))

  def inProgressTaskListLiaisonOfficer(id: String, fullName: String = "Started"): LiaisonOfficer =
    LiaisonOfficer(id, fullName = Some(fullName))

  def completeTaskListLiaisonOfficer(id: String, fullName: String = "Complete Liaison Officer"): LiaisonOfficer =
    LiaisonOfficer(
      id = id,
      fullName = Some(fullName),
      phoneNumber = Some("01234567890"),
      communication = Set(ByEmail),
      email = Some("liaison@example.com")
    )

  def liaisonOfficersWith(liaisonOfficers: LiaisonOfficer*): LiaisonOfficers =
    LiaisonOfficers(liaisonOfficers)

  def inProgressTaskListSignatory(id: String, fullName: String = "Started"): Signatory =
    Signatory(id, fullName = Some(fullName))

  def completeTaskListSignatory(id: String, fullName: String = "Complete Signatory"): Signatory =
    Signatory(
      id = id,
      fullName = Some(fullName),
      jobTitle = Some("Director")
    )

  def signatoriesWith(signatories: Signatory*): Signatories =
    Signatories(signatories)

  def inProgressTaskListThirdParty(id: String, name: String = "Started"): ThirdParty =
    ThirdParty(id, thirdPartyName = Some(name))

  def completeTaskListThirdParty(id: String, name: String = "Complete Third Party"): ThirdParty =
    ThirdParty(
      id = id,
      thirdPartyName = Some(name),
      managingIsaReturns = Some(YesNoAnswer.No),
      usingInvestorFunds = Some(YesNoAnswer.No)
    )

  val thirdPartyOrganisationsNotUsed: ThirdPartyOrganisations =
    ThirdPartyOrganisations(managedByThirdParty = Some(YesNoAnswer.No))

  def testThirdPartyOrganisations(
    thirdParties: Seq[ThirdParty],
    managedByThirdParty: YesNoAnswer = YesNoAnswer.Yes,
    connectedOrganisations: Seq[String] = Seq.empty
  ): ThirdPartyOrganisations =
    ThirdPartyOrganisations(
      managedByThirdParty = Some(managedByThirdParty),
      thirdParties = thirdParties,
      connectedOrganisations = connectedOrganisations
    )

  def journeyDataWithThirdParties(thirdParties: Seq[ThirdParty]): JourneyData =
    testJourneyData.copy(
      thirdPartyOrganisations = Some(testThirdPartyOrganisations(thirdParties))
    )

  def completeTaskListJourneyData: JourneyData =
    emptyJourneyDataWithBusinessVerification.copy(
      organisationDetails = Some(completeTaskListOrganisationDetails),
      organisationEmail = Some(completeTaskListOrganisationEmail),
      isaProducts = Some(completeTaskListIsaProducts),
      certificatesOfAuthority = Some(completeTaskListCertificatesOfAuthority),
      liaisonOfficers = Some(LiaisonOfficers(Seq(completeTaskListLiaisonOfficer("lo-1")))),
      signatories = Some(Signatories(Seq(completeTaskListSignatory("sig-1")))),
      thirdPartyOrganisations = Some(thirdPartyOrganisationsNotUsed)
    )

  val testJourneyData: JourneyData =
    JourneyData(
      groupId = testGroupId,
      enrolmentId = testEnrolmentId,
      isaProducts = Some(testIsaProductsAnswers),
      organisationDetails = Some(testOrganisationDetails),
      certificatesOfAuthority = Some(testCoaAnswersWithArticles),
      liaisonOfficers = Some(testLiaisonOfficers),
      signatories = Some(testSignatories)
    )

  val testRegisteredAddress: RegisteredAddress = RegisteredAddress(
    addressLine1 = Some("testLine1"),
    addressLine2 = Some("test town"),
    addressLine3 = Some("test city"),
    postCode = Some("AA11AA"),
    uprn = None
  )

  val testCorrespondenceAddress: CorrespondenceAddress = CorrespondenceAddress(
    addressLine1 = Some("testLine1"),
    addressLine2 = Some("test town"),
    addressLine3 = Some("test city"),
    postCode = Some("AA11AA")
  )

  val testBV: BusinessVerification = BusinessVerification(
    businessRegistrationPassed = Some(true),
    businessVerificationPassed = Some(true),
    ctUtr = Some("1234567890"),
    registeredAddress = Some(testRegisteredAddress),
    companyName = Some(testString),
    companyNumber = Some(testString),
    businessPartnerId = Some(testString)
  )
}
