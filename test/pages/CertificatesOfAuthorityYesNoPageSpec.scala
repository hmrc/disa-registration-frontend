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

package pages

import base.SpecBase
import models.journeydata.certificatesofauthority.*
import models.journeydata.certificatesofauthority.CertificatesOfAuthorityYesNo.*
import models.journeydata.certificatesofauthority.FcaArticles.Article14
import models.journeydata.certificatesofauthority.FinancialOrganisation.EuropeanInstitution

class CertificatesOfAuthorityYesNoPageSpec extends SpecBase {

  "CertificatesOfAuthorityYesNoPage" - {

    "must have the correct string representation" in {
      CertificatesOfAuthorityYesNoPage.toString mustBe "certificatesOfAuthorityYesNo"
    }

    "clearAnswer" - {

      "must set certificatesYesNo to None" in {

        val section =
          CertificatesOfAuthority(certificatesYesNo = Some(Yes))

        val result =
          CertificatesOfAuthorityYesNoPage.clearAnswer(section)

        result.certificatesYesNo mustBe None
      }
    }

    "pagesToClear" - {

      "must return FinancialOrganisationPage when answer is Yes" in {

        val section =
          CertificatesOfAuthority(certificatesYesNo = Some(Yes))

        val result =
          CertificatesOfAuthorityYesNoPage.pagesToClear(section)

        result mustBe List(FinancialOrganisationPage)
      }

      "must return FcaArticlesPage when answer is No" in {

        val section =
          CertificatesOfAuthority(certificatesYesNo = Some(No))

        val result =
          CertificatesOfAuthorityYesNoPage.pagesToClear(section)

        result mustBe List(FcaArticlesPage)
      }

      "must return Nil when answer is None" in {

        val section =
          CertificatesOfAuthority(certificatesYesNo = None)

        val result =
          CertificatesOfAuthorityYesNoPage.pagesToClear(section)

        result mustBe Nil
      }
    }

    "resumeNormalMode" - {

      "must return true when Yes is selected and fcaArticles is empty" in {

        val section =
          CertificatesOfAuthority(
            certificatesYesNo = Some(Yes),
            fcaArticles = None,
            financialOrganisation = None
          )

        val result =
          CertificatesOfAuthorityYesNoPage.resumeNormalMode(section)

        result mustBe true
      }

      "must return false when Yes is selected and fcaArticles already has answers" in {

        val section =
          CertificatesOfAuthority(
            certificatesYesNo = Some(Yes),
            fcaArticles = Some(Seq(Article14))
          )

        val result =
          CertificatesOfAuthorityYesNoPage.resumeNormalMode(section)

        result mustBe false
      }

      "must return true when No is selected and financialOrganisation is empty" in {

        val section =
          CertificatesOfAuthority(
            certificatesYesNo = Some(No),
            financialOrganisation = None
          )

        val result =
          CertificatesOfAuthorityYesNoPage.resumeNormalMode(section)

        result mustBe true
      }

      "must return false when No is selected and financialOrganisation already has answers" in {

        val section =
          CertificatesOfAuthority(
            certificatesYesNo = Some(No),
            financialOrganisation = Some(Seq(EuropeanInstitution))
          )

        val result =
          CertificatesOfAuthorityYesNoPage.resumeNormalMode(section)

        result mustBe false
      }

      "must return false when certificatesYesNo is None" in {

        val section =
          CertificatesOfAuthority(certificatesYesNo = None)

        val result =
          CertificatesOfAuthorityYesNoPage.resumeNormalMode(section)

        result mustBe false
      }
    }
  }
}
