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

package models.summaryModels

import base.SpecBase
import controllers.routes
import models.CheckMode
import models.FcaArticles.{Article14, Article21, Article25}
import models.journeydata.{CertificatesOfAuthority, JourneyData}
import viewmodels.checkAnswers.FcaArticlesSummary
import org.scalatest.matchers.must.Matchers.*

class FcaArticlesSummarySpec extends SpecBase {

  "FcaArticlesSummary.row" - {

    "return None when no FCA articles exist" in {

      val answers = JourneyData(groupId = testGroupId, enrolmentId = testEnrolmentId, certificatesOfAuthority = None)

      FcaArticlesSummary.row(answers) mustBe None
    }

    "render a row when FCA articles exist" in {

      val answers = JourneyData(
        groupId = testGroupId,
        enrolmentId = testEnrolmentId,
        certificatesOfAuthority =
          Some(CertificatesOfAuthority(fcaArticles = Some(Seq(Article14, Article21, Article25))))
      )

      val result = FcaArticlesSummary.row(answers)

      result mustBe defined

      val row = result.value

      row.key.content.asHtml.toString must include(
        messages("fcaArticles.checkYourAnswersLabel")
      )

      row.value.content.asHtml.toString must include(
        messages("fcaArticles.article14")
      )

      row.value.content.asHtml.toString must include(
        messages("fcaArticles.article21")
      )

      row.value.content.asHtml.toString must include(
        messages("fcaArticles.article25")
      )

      row.actions.get.items.head.href mustBe
        routes.FcaArticlesController.onPageLoad(CheckMode).url
    }
  }
}
