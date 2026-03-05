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
import models.journeydata.JourneyData
import models.journeydata.certificatesofauthority.CertificatesOfAuthority
import models.journeydata.certificatesofauthority.CertificatesOfAuthorityYesNo.{No, Yes}
import models.journeydata.certificatesofauthority.FcaArticles.Article14
import models.journeydata.certificatesofauthority.FinancialOrganisation.EuropeanInstitution
import models.journeydata.isaproducts.{InnovativeFinancialProduct, IsaProduct, IsaProducts}
import uk.gov.hmrc.auth.core.User
import uk.gov.hmrc.auth.core.retrieve.Credentials

import scala.util.Random

trait TestData extends Generators {
  val testString             = "test"
  val testGroupId            = "3147318d-1cd9-4534-a4e8-ae268ea923ed"
  val testEnrolmentId        = "2b2825af-d5a6-4518-a6cb-67ddb4e66952"
  val testZRef               = s"Z${(1 to 4).map(_ => Random.nextInt(10)).mkString}"
  val testCredentials        = Credentials(testString, testString)
  val testCredentialRoleUser = User

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

  val testCoaAnswersWithArticles: CertificatesOfAuthority =
    CertificatesOfAuthority(
      certificatesYesNo = Some(Yes),
      fcaArticles = Some(Seq(Article14))
    )

  val testJourneyData: JourneyData =
    JourneyData(groupId = testGroupId, enrolmentId = testEnrolmentId, isaProducts = Some(testIsaProductsAnswers))
}
