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
import models.journeydata.certificatesofauthority.{CertificatesOfAuthority, CertificatesOfAuthorityYesNo, FcaArticles, FinancialOrganisation}
import models.journeydata.isaproducts.{InnovativeFinancialProduct, IsaProduct, IsaProducts}
import models.journeydata.liaisonofficers.{LiaisonOfficer, LiaisonOfficerCommunication, LiaisonOfficers}
import models.journeydata.signatories.{Signatories, Signatory}
import models.journeydata.thirdparty.{ThirdParty, ThirdPartyOrganisations}
import models.journeydata.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsObject, Json}

class EnrolmentSubmittedEventPayloadSpec extends AnyFreeSpec with Matchers {

  private val fullJourneyData = JourneyData(
    groupId = "group-id",
    enrolmentId = "enrolment-id",
    formBundleId = Some("XMADP0000000208"),
    businessVerification = Some(
      BusinessVerification(
        businessRegistrationPassed = Some(true),
        businessVerificationPassed = Some(true),
        utr = Some("1234567890"),
        registeredAddress = None,
        companyName = Some("Gringotts Bank"),
        businessPartnerId = Some("XA0001234567890"),
        companyNumber = Some("12345678"),
        companyType = Some(GrsCompanyType.LimitedCompany)
      )
    ),
    organisationDetails = Some(
      OrganisationDetails(
        registeredToManageIsa = Some(YesNoAnswer.Yes),
        zRefNumber = Some("Z1234"),
        tradingUsingDifferentName = Some(YesNoAnswer.Yes),
        tradingName = Some("Gotts Bank"),
        fcaNumber = Some("1234567"),
        registeredAddressCorrespondence = Some(YesNoAnswer.No),
        correspondenceAddress = Some(
          CorrespondenceAddress(
            addressLine1 = Some("72 Diagon Alley"),
            addressLine2 = Some("London"),
            postCode = Some("SE23 6FH"),
            uprn = Some("10034781602")
          )
        ),
        orgTelephoneNumber = Some("07886816816")
      )
    ),
    organisationEmail = Some(OrganisationEmail(Some("im@tsbbank.com"), Some(true))),
    isaProducts = Some(
      IsaProducts(
        isaProducts = Some(IsaProduct.values),
        p2pPlatform = Some("platform1"),
        p2pPlatformNumber = Some("1234567"),
        innovativeFinancialProducts = Some(InnovativeFinancialProduct.values)
      )
    ),
    certificatesOfAuthority = Some(
      CertificatesOfAuthority(
        certificatesYesNo = Some(CertificatesOfAuthorityYesNo.Yes),
        fcaArticles = Some(Seq(FcaArticles.Article14, FcaArticles.Article36H)),
        financialOrganisation = Some(Seq(FinancialOrganisation.Bank))
      )
    ),
    liaisonOfficers = Some(
      LiaisonOfficers(
        Seq(
          LiaisonOfficer(
            id = "officer-1",
            fullName = Some("John Doe"),
            phoneNumber = Some("07886816816"),
            communication = Set(LiaisonOfficerCommunication.ByPost, LiaisonOfficerCommunication.ByEmail),
            email = Some("John.Doe@testIm.com")
          )
        )
      )
    ),
    signatories = Some(
      Signatories(Seq(Signatory(id = "signatory-1", fullName = Some("John Doe"), jobTitle = Some("Compliance Manager"))))
    ),
    thirdPartyOrganisations = Some(
      ThirdPartyOrganisations(
        managedByThirdParty = Some(YesNoAnswer.Yes),
        thirdParties = Seq(
          ThirdParty(
            id = "third-party-1",
            thirdPartyName = Some("gringo"),
            thirdPartyFrn = Some("123456"),
            managingIsaReturns = Some(YesNoAnswer.Yes),
            usingInvestorFunds = Some(YesNoAnswer.Yes),
            investorFundsPercentage = Some("1")
          ),
          ThirdParty(
            id = "third-party-2",
            thirdPartyName = Some("tsb"),
            managingIsaReturns = Some(YesNoAnswer.No),
            usingInvestorFunds = Some(YesNoAnswer.No)
          )
        ),
        connectedOrganisations = Seq("gringo", "tsb")
      )
    )
  )

  private def payloadJson(journeyData: JourneyData): JsObject =
    Json.toJsObject(EnrolmentSubmittedEventPayload.fromJourneyData(journeyData))

  "EnrolmentSubmittedEventPayload" - {

    "must write every section of a completed journey in the audit format" in {
      payloadJson(fullJourneyData) mustBe Json.obj(
        "subscriptionId"            -> "XMADP0000000208",
        "groupType"                 -> "Limited company",
        "isaManagerProviderDetails" -> Json.obj(
          "existingManager"           -> "Yes",
          "zReference"                -> "Z1234",
          "tradingUsingDifferentName" -> "Yes",
          "tradingName"               -> "Gotts Bank",
          "fcaNumber"                 -> "1234567",
          "useRegisteredAddress"      -> "No",
          "correspondenceAddress"     -> Json.obj(
            "UPRN"         -> "10034781602",
            "addressLine1" -> "72 Diagon Alley",
            "addressLine2" -> "London",
            "postCode"     -> "SE23 6FH"
          ),
          "orgTelephoneNumber"        -> "07886816816"
        ),
        "orgEmailAddress"           -> "im@tsbbank.com",
        "liaisonOfficers"           -> Json.arr(
          Json.obj(
            "name"                    -> "John Doe",
            "emailAddress"            -> "John.Doe@testIm.com",
            "telephoneNumber"         -> "07886816816",
            "communicationPreference" -> Json.obj("post" -> true, "email" -> true, "telephone" -> false)
          )
        ),
        "signatories"               -> Json.arr(Json.obj("name" -> "John Doe", "position" -> "Compliance Manager")),
        "isaProducts"               -> Json.obj(
          "cashISA"                     -> true,
          "cashJuniorISA"               -> true,
          "stocksSharesISA"             -> true,
          "stocksSharesJuniorISA"       -> true,
          "innovativeFinanceISA"        -> true,
          "innovativeFinanceISADetails" -> Json.arr(
            Json.obj(
              "loans36HPermissions"                -> true,
              "loansPlatform36HPermissions"        -> true,
              "loansPlatform36HPermissionsDetails" -> Json.arr(
                Json.obj("nameOfPlatform" -> "platform1", "platformFcaNumber" -> 1234567)
              ),
              "crowdfundedDebentures"              -> true,
              "ltafRedemption"                     -> true
            )
          )
        ),
        "certificateAuthority"      -> Json.obj(
          "certificationDeclared" -> "Yes",
          "articlesApply"         -> Json.arr(Json.obj("article14" -> true), Json.obj("article36H" -> true))
        ),
        "furtherInformation"        -> Json.obj(
          "outsourcedArrangements"        -> "Yes",
          "outsourcedArrangementsDetails" -> Json.arr(
            Json.obj(
              "nameOfFirm"                  -> "gringo",
              "fcaNumber"                   -> "123456",
              "returnsReportingOutsourced"  -> "Yes",
              "feesCommissionAndIncentives" -> "Yes",
              "percentageCommission"        -> "1%"
            ),
            Json.obj(
              "nameOfFirm"                  -> "tsb",
              "returnsReportingOutsourced"  -> "No",
              "feesCommissionAndIncentives" -> "No"
            )
          ),
          "connectedParties"              -> "Yes",
          "connectedPartiesFirms"         -> Json.arr("gringo", "tsb")
        )
      )
    }

    "must leave out answers that only apply to a different choice" in {
      val journeyData = fullJourneyData.copy(
        organisationDetails = fullJourneyData.organisationDetails.map(
          _.copy(registeredToManageIsa = Some(YesNoAnswer.No), tradingUsingDifferentName = Some(YesNoAnswer.No))
        ),
        isaProducts = Some(IsaProducts(isaProducts = Some(Seq(IsaProduct.CashIsas)))),
        certificatesOfAuthority = Some(
          CertificatesOfAuthority(
            certificatesYesNo = Some(CertificatesOfAuthorityYesNo.No),
            fcaArticles = Some(Seq(FcaArticles.Article14)),
            financialOrganisation = Some(Seq(FinancialOrganisation.Bank, FinancialOrganisation.AssuranceUndertaking))
          )
        ),
        thirdPartyOrganisations = Some(ThirdPartyOrganisations(managedByThirdParty = Some(YesNoAnswer.No)))
      )

      val json = payloadJson(journeyData)

      (json \ "isaManagerProviderDetails" \ "existingManager").as[String] mustBe "No"
      (json \ "isaManagerProviderDetails" \ "zReference").toOption mustBe None
      (json \ "isaManagerProviderDetails" \ "tradingUsingDifferentName").as[String] mustBe "No"
      (json \ "isaManagerProviderDetails" \ "tradingName").toOption mustBe None
      (json \ "isaProducts" \ "innovativeFinanceISA").as[Boolean] mustBe false
      (json \ "isaProducts" \ "innovativeFinanceISADetails").toOption mustBe None
      (json \ "certificateAuthority").as[JsObject] mustBe Json.obj(
        "certificationDeclared" -> "No",
        "typeOfOrganisation"    -> Json.arr(Json.obj("bank" -> true), Json.obj("assuranceUndertakingServices" -> true))
      )
      (json \ "furtherInformation").as[JsObject] mustBe Json.obj("outsourcedArrangements" -> "No")
    }

    "must leave out platform details when loans are not made through a 36H platform" in {
      val journeyData = fullJourneyData.copy(
        isaProducts = Some(
          IsaProducts(
            isaProducts = Some(Seq(IsaProduct.InnovativeFinanceIsas)),
            innovativeFinancialProducts = Some(Seq(InnovativeFinancialProduct.CrowdFundedDebentures))
          )
        )
      )

      (payloadJson(journeyData) \ "isaProducts" \ "innovativeFinanceISADetails" \ 0).as[JsObject] mustBe Json.obj(
        "loans36HPermissions"         -> false,
        "loansPlatform36HPermissions" -> false,
        "crowdfundedDebentures"       -> true,
        "ltafRedemption"              -> false
      )
    }

    "must write an empty object when no journey sections have been answered" in {
      payloadJson(JourneyData(groupId = "group-id", enrolmentId = "enrolment-id")) mustBe Json.obj()
    }
  }
}
