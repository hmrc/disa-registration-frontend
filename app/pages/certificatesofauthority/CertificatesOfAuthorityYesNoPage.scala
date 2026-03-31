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

package pages.certificatesofauthority

import models.journeydata.certificatesofauthority.CertificatesOfAuthority
import models.journeydata.certificatesofauthority.CertificatesOfAuthorityYesNo.{No, Yes}
import pages.{ClearablePage, PageWithDependents}

case object CertificatesOfAuthorityYesNoPage extends PageWithDependents[CertificatesOfAuthority] {

  override def toString: String = "certificatesOfAuthorityYesNo"

  def clearAnswer(answers: CertificatesOfAuthority): CertificatesOfAuthority =
    answers.copy(certificatesYesNo = None)

  def pagesToClear(
    currentAnswers: CertificatesOfAuthority
  ): List[ClearablePage[CertificatesOfAuthority]] =
    currentAnswers.certificatesYesNo match {
      case Some(Yes) =>
        List(FinancialOrganisationPage)
      case Some(No)  =>
        List(FcaArticlesPage)
      case _         =>
        Nil
    }

  def resumeNormalMode(answers: CertificatesOfAuthority): Boolean =
    answers match {
      case CertificatesOfAuthority(Some(Yes), fcaArticles, _)          =>
        fcaArticles.forall(_.isEmpty)
      case CertificatesOfAuthority(Some(No), _, financialOrganisation) =>
        financialOrganisation.forall(_.isEmpty)
      case _                                                           => false
    }
}
