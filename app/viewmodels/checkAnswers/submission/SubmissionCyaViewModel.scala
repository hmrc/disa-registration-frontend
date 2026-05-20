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

package viewmodels.checkAnswers.submission

import controllers.certificatesofauthority.routes.{CertificatesOfAuthorityYesNoController, FcaArticlesController, FinancialOrganisationController}
import controllers.isaproducts.routes.{InnovativeFinancialProductsController, IsaProductsController, PeerToPeerPlatformController, PeerToPeerPlatformNumberController}
import controllers.liaisonofficers.routes.AddedLiaisonOfficersController
import controllers.orgdetails.routes.*
import controllers.orgemail.routes.OrganisationEmailAddressController
import controllers.signatories.routes.AddedSignatoryController
import models.ReturnTo.SubmissionCya
import models.YesNoAnswer.No
import models.journeydata.thirdparty.ThirdParty
import models.journeydata.{CorrespondenceAddress, JourneyData, RegisteredAddress}
import models.{CheckMode, ReturnTo, YesNoAnswer}
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow, Value}
import viewmodels.checkAnswers.liaisonofficers.{LiaisonOfficerCommunicationSummary, LiaisonOfficerEmailSummary, LiaisonOfficerNameSummary, LiaisonOfficerPhoneNumberSummary}
import viewmodels.checkAnswers.signatories.{SignatoryJobTitleSummary, SignatoryNameSummary}
import viewmodels.checkAnswers.thirdparty.finalcya.*
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

case class SubmissionCyaSection(
                                 heading: String,
                                 rows: Seq[SummaryListRow],
                                 headingLevel: Int = 2,
                                 renderWhenEmpty: Boolean = false
                               )

case class SubmissionCyaViewModel(
                                   sections: Seq[SubmissionCyaSection]
                                 )

object SubmissionCyaViewModel {

  def apply(answers: JourneyData)(implicit messages: Messages): SubmissionCyaViewModel = {
    val returnTo = Some(SubmissionCya)

    val sections =
      Seq(
        SubmissionCyaSection(
          messages("submissionCya.organisationInformation.heading"),
          organisationInformationRows(answers, returnTo)
        ),
        SubmissionCyaSection(
          messages("submissionCya.organisationEmail.heading"),
          organisationEmailRows(answers, returnTo)
        ),
        SubmissionCyaSection(
          messages("submissionCya.productsAndCertificates.heading"),
          productsAndCertificatesRows(answers, returnTo)
        )
      ) ++
        authorisedUsersSections(answers, returnTo) ++
        thirdPartyOrganisationSections(answers, returnTo)

    SubmissionCyaViewModel(
      sections = sections.filter(section => section.rows.nonEmpty || section.renderWhenEmpty)
    )
  }

  private def organisationInformationRows(answers: JourneyData, returnTo: Option[ReturnTo])(implicit messages: Messages): Seq[SummaryListRow] = {
    val businessVerification = answers.businessVerification
    val organisationDetails  = answers.organisationDetails
    val correspondenceRow: Option[SummaryListRow] =
      organisationDetails.flatMap(_.registeredAddressCorrespondence) match {
        case Some(No) =>
          organisationDetails.flatMap(_.correspondenceAddress).flatMap { address =>
            addressRow(
              key = "submissionCya.correspondenceAddress.label",
              address = address,
              href = Some(EnterYourOrganisationAddressController.onPageLoad(CheckMode, returnTo).url),
              hidden = Some("registeredAddressCorrespondence.change.hidden"))}
        case _ =>
          None
      }

    Seq(
      businessVerification.flatMap(_.companyName).map(staticRow("submissionCya.organisationName.label", _)),
      businessVerification.flatMap(_.companyNumber).map(staticRow("submissionCya.registrationNumber.label", _)),
      businessVerification.flatMap(_.ctUtr).map(staticRow("submissionCya.utrNumber.label", _)),
      businessVerification.flatMap(_.registeredAddress).flatMap(addressRow("submissionCya.registeredAddress.label", _)),
      organisationDetails.flatMap(_.registeredToManageIsa).map { answer =>
        changeRow(
          key = "submissionCya.registeredToManageIsas.label",
          value = yesNo(answer),
          href = RegisteredIsaManagerController.onPageLoad(CheckMode, returnTo).url,
          hidden = "registeredIsaManager.change.hidden"
        )
      },
      organisationDetails.flatMap(_.zRefNumber).map { answer =>
        changeRow(
          key = "submissionCya.zReference.label",
          value = text(answer),
          href = ZReferenceNumberController.onPageLoad(CheckMode, returnTo).url,
          hidden = "zReferenceNumber.change.hidden"
        )
      },
      organisationDetails.flatMap(_.tradingUsingDifferentName).map { answer =>
        changeRow(
          key = "submissionCya.differentTradingName.label",
          value = yesNo(answer),
          href = TradingUsingDifferentNameController.onPageLoad(CheckMode, returnTo).url,
          hidden = "tradingUsingDifferentName.change.hidden"
        )
      },
      organisationDetails.flatMap(_.tradingName).map { answer =>
        changeRow(
          key = "submissionCya.tradingName.label",
          value = text(answer),
          href = TradingNameController.onPageLoad(CheckMode, returnTo).url,
          hidden = "tradingName.change.hidden"
        )
      },
      organisationDetails.flatMap(_.fcaNumber).map { answer =>
        changeRow(
          key = "submissionCya.fcaNumber.label",
          value = text(answer),
          href = FirmReferenceNumberController.onPageLoad(CheckMode, returnTo).url,
          hidden = "firmReferenceNumber.change.hidden"
        )
      },
      organisationDetails.flatMap(_.registeredAddressCorrespondence).map { answer =>
        changeRow(
          key = "submissionCya.registeredAddressCorrespondence.label",
          value = yesNo(answer),
          href = RegisteredAddressCorrespondenceController.onPageLoad(CheckMode, returnTo).url,
          hidden = "registeredAddressCorrespondence.change.hidden"
        )
      },
      correspondenceRow,
      organisationDetails.flatMap(_.orgTelephoneNumber).map { answer =>
        changeRow(
          key = "submissionCya.telephoneNumber.label",
          value = text(answer),
          href = OrganisationTelephoneNumberController.onPageLoad(CheckMode, returnTo).url,
          hidden = "organisationTelephoneNumber.change.hidden"
        )
      }
    ).flatten
  }

  private def organisationEmailRows(answers: JourneyData, returnTo: Option[ReturnTo])(implicit
                                                                                      messages: Messages
  ): Seq[SummaryListRow] =
    answers.organisationEmail
      .flatMap(_.organisationEmail)
      .map { email =>
        changeRow(
          key = "submissionCya.organisationEmail.label",
          value = text(email),
          href = OrganisationEmailAddressController.onPageLoad(CheckMode, returnTo).url,
          hidden = "organisationEmailAddress.change.hidden"
        )
      }
      .toSeq

  private def productsAndCertificatesRows(answers: JourneyData, returnTo: Option[ReturnTo])(implicit
                                                                                            messages: Messages
  ): Seq[SummaryListRow] = {
    val isaProducts             = answers.isaProducts
    val certificatesOfAuthority = answers.certificatesOfAuthority

    Seq(
      isaProducts.flatMap(_.isaProducts).map { selected =>
        changeRow(
          key = "submissionCya.isaProducts.label",
          value = multiValue(selected.map(answer => messages(s"isaProducts.$answer"))),
          href = IsaProductsController.onPageLoad(CheckMode, returnTo).url,
          hidden = "isaProducts.change.hidden"
        )
      },
      isaProducts.flatMap(_.innovativeFinancialProducts).map { selected =>
        changeRow(
          key = "submissionCya.innovativeFinanceProducts.label",
          value = multiValue(selected.map(answer => messages(s"innovativeFinancialProducts.$answer"))),
          href = InnovativeFinancialProductsController.onPageLoad(CheckMode, returnTo).url,
          hidden = "innovativeFinancialProducts.change.hidden"
        )
      },
      isaProducts.flatMap(_.p2pPlatform).map { answer =>
        changeRow(
          key = "submissionCya.platformName.label",
          value = text(answer),
          href = PeerToPeerPlatformController.onPageLoad(CheckMode, returnTo).url,
          hidden = "peerToPeerPlatform.change.hidden"
        )
      },
      isaProducts.flatMap(_.p2pPlatformNumber).map { answer =>
        changeRow(
          key = "peerToPeerPlatformNumber.checkYourAnswersLabel",
          value = text(answer),
          href = PeerToPeerPlatformNumberController.onPageLoad(CheckMode, returnTo).url,
          hidden = "peerToPeerPlatformNumber.change.hidden"
        )
      },
      certificatesOfAuthority.flatMap(_.certificatesYesNo).map { answer =>
        changeRow(
          key = "certificatesOfAuthorityYesNo.checkYourAnswersLabel",
          value = text(messages(s"certificatesOfAuthorityYesNo.$answer")),
          href = CertificatesOfAuthorityYesNoController.onPageLoad(CheckMode, returnTo).url,
          hidden = "certificatesOfAuthorityYesNo.change.hidden"
        )
      },
      certificatesOfAuthority.flatMap(_.fcaArticles).map { selected =>
        changeRow(
          key = "fcaArticles.checkYourAnswersLabel",
          value = multiValue(selected.map(answer => messages(s"fcaArticles.$answer"))),
          href = FcaArticlesController.onPageLoad(CheckMode, returnTo).url,
          hidden = "fcaArticles.change.hidden"
        )
      },
      certificatesOfAuthority.flatMap(_.financialOrganisation).map { selected =>
        changeRow(
          key = "financialOrganisation.checkYourAnswersLabel",
          value = multiValue(selected.map(answer => messages(s"financialOrganisation.$answer"))),
          href = FinancialOrganisationController.onPageLoad(CheckMode, returnTo).url,
          hidden = "financialOrganisation.change.hidden"
        )
      }
    ).flatten
  }

  private def authorisedUsersSections(answers: JourneyData, returnTo: Option[ReturnTo])(implicit
                                                                                        messages: Messages
  ): Seq[SubmissionCyaSection] =
    liaisonOfficerSections(answers, returnTo) ++ signatorySections(answers, returnTo)

  private def liaisonOfficerSections(answers: JourneyData, returnTo: Option[ReturnTo])(implicit
                                                                                       messages: Messages
  ): Seq[SubmissionCyaSection] = {
    val liaisonOfficers = answers.liaisonOfficers.toSeq.flatMap(_.liaisonOfficers.filterNot(_.inProgress))

    if (liaisonOfficers.isEmpty) Nil
    else
      Seq(SubmissionCyaSection(messages("submissionCya.liaisonOfficer.heading"), Nil, renderWhenEmpty = true)) ++
        liaisonOfficers.zipWithIndex.map { case (liaisonOfficer, index) =>
          SubmissionCyaSection(
            messages("submissionCya.liaisonOfficer.subheading", index + 1),
            Seq(
              LiaisonOfficerNameSummary.row(liaisonOfficer, returnTo),
              LiaisonOfficerEmailSummary.row(liaisonOfficer, returnTo),
              LiaisonOfficerPhoneNumberSummary.row(liaisonOfficer, returnTo),
              LiaisonOfficerCommunicationSummary.row(liaisonOfficer, returnTo)
            ).flatten,
            headingLevel = 3
          )
        } ++
        Seq(
          SubmissionCyaSection(
            messages("submissionCya.addAnother.heading"),
            Seq(
              addAnotherRow(
                key = "submissionCya.addAnotherLiaisonOfficer.label",
                href = AddedLiaisonOfficersController.onPageLoad(CheckMode, returnTo).url,
                hidden = "submissionCya.addAnotherLiaisonOfficer.change.hidden"
              )
            )
          )
        )
  }

  private def signatorySections(answers: JourneyData, returnTo: Option[ReturnTo])(implicit
                                                                                  messages: Messages
  ): Seq[SubmissionCyaSection] = {
    val signatories = answers.signatories.toSeq.flatMap(_.signatories.filterNot(_.inProgress))

    if (signatories.isEmpty) Nil
    else
      Seq(SubmissionCyaSection(messages("submissionCya.signatory.heading"), Nil, renderWhenEmpty = true)) ++
        signatories.zipWithIndex.map { case (signatory, index) =>
          SubmissionCyaSection(
            messages("submissionCya.signatory.subheading", index + 1),
            Seq(
              SignatoryNameSummary.row(signatory, returnTo),
              SignatoryJobTitleSummary.row(signatory, returnTo)
            ).flatten,
            headingLevel = 3
          )
        } ++
        Seq(
          SubmissionCyaSection(
            messages("submissionCya.addAnother.heading"),
            Seq(
              addAnotherRow(
                key = "submissionCya.addAnotherSignatory.label",
                href = AddedSignatoryController.onPageLoad(CheckMode, returnTo).url,
                hidden = "submissionCya.addAnotherSignatory.change.hidden"
              )
            )
          )
        )
  }

  private def thirdPartyOrganisationSections(answers: JourneyData, returnTo: Option[ReturnTo])(implicit
                                                                                               messages: Messages
  ): Seq[SubmissionCyaSection] =
    answers.thirdPartyOrganisations.toSeq.flatMap { section =>
      val topRows = ProductsManagedByThirdPartySummary.row(section, returnTo).toSeq

      val thirdPartySections =
        section.completedThirdParties.zipWithIndex.map { case (thirdParty, index) =>
          val displayIndex = index + 1
          SubmissionCyaSection(
            messages("thirdPartiesCheckYourAnswers.sub.heading", displayIndex),
            thirdPartyRows(thirdParty, displayIndex, returnTo),
            headingLevel = 3
          )
        }

      val connectedOrganisationsSections =
        Option
          .when(section.completedCount > 1) {
            SubmissionCyaSection(
              messages("thirdPartiesCheckYourAnswers.connectedOrganisations.sub.heading"),
              ThirdPartyConnectedOrganisationsControllerSummary.row(section, returnTo).toSeq,
              headingLevel = 3
            )
          }
          .toSeq

      val addAnotherSections =
        Option
          .when(section.completedThirdParties.nonEmpty) {
            SubmissionCyaSection(
              messages("thirdPartiesCheckYourAnswers.addAnother.sub.heading"),
              Seq(AddAnotherThirdPartySummary.row(section, returnTo))
            )
          }
          .toSeq

      Seq(SubmissionCyaSection(messages("submissionCya.thirdPartyOrganisations.heading"), topRows)) ++
        thirdPartySections ++
        connectedOrganisationsSections ++
        addAnotherSections
    }

  private def thirdPartyRows(thirdParty: ThirdParty, index: Int, returnTo: Option[ReturnTo])(implicit
                                                                                             messages: Messages
  ): Seq[SummaryListRow] =
    Seq(
      ThirdPartyOrgDetailsSummary.row(thirdParty, index, returnTo),
      ThirdPartyManagingReturnsSummary.row(thirdParty, returnTo),
      InvestorFundsUsedByThirdPartySummary.row(thirdParty, returnTo),
      ThirdPartyInvestorFundsPercentageSummary.row(thirdParty, returnTo)
    ).flatten

  private def addAnotherRow(key: String, href: String, hidden: String)(implicit messages: Messages): SummaryListRow =
    changeRow(
      key = key,
      value = text(messages("site.no")),
      href = href,
      hidden = hidden
    )

  private def staticRow(key: String, answer: String)(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = Key(Text(messages(key))),
      value = text(answer)
    )

  private def changeRow(key: String, value: Value, href: String, hidden: String)(implicit
                                                                                 messages: Messages
  ): SummaryListRow =
    SummaryListRowViewModel(
      key = Key(Text(messages(key))),
      value = value,
      actions = Seq(
        ActionItemViewModel("site.change", href)
          .withVisuallyHiddenText(messages(hidden))
      )
    )

  private def text(answer: String): Value =
    ValueViewModel(HtmlContent(HtmlFormat.escape(answer)))

  private def yesNo(answer: YesNoAnswer)(implicit messages: Messages): Value =
    text(messages(s"site.$answer"))

  private def multiValue(answers: Seq[String]): Value =
    ValueViewModel(
      HtmlContent(
        Html(
          answers
            .map(answer => HtmlFormat.escape(answer).body)
            .mkString("<br>")
        )
      )
    )

  private def addressRow(key: String, address: RegisteredAddress)(implicit messages: Messages): Option[SummaryListRow] =
    addressLines(address).map { lines =>
      SummaryListRowViewModel(
        key = Key(Text(messages(key))),
        value = multiValue(lines)
      )
    }

  private def addressRow(
                          key: String,
                          address: CorrespondenceAddress,
                          href: Option[String],
                          hidden: Option[String]
                        )(implicit messages: Messages): Option[SummaryListRow] =
    addressLines(address).map { lines =>
      href.zip(hidden).headOption match {
        case Some((url, hiddenMessage)) =>
          changeRow(key, multiValue(lines), url, hiddenMessage)
        case None                       =>
          SummaryListRowViewModel(
            key = Key(Text(messages(key))),
            value = multiValue(lines)
          )
      }
    }

  private def addressLines(address: RegisteredAddress): Option[Seq[String]] = {
    val lines = Seq(address.addressLine1, address.addressLine2, address.addressLine3, address.postCode).flatten
    Option.when(lines.nonEmpty)(lines)
  }

  private def addressLines(address: CorrespondenceAddress): Option[Seq[String]] = {
    val lines = Seq(address.addressLine1, address.addressLine2, address.addressLine3, address.postCode).flatten
    Option.when(lines.nonEmpty)(lines)
  }
}