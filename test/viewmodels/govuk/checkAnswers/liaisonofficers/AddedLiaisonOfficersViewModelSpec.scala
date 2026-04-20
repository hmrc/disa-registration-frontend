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

package viewmodels.govuk.checkAnswers.liaisonofficers

import base.SpecBase
import config.FrontendAppConfig
import controllers.liaisonofficers.routes.{LiaisonOfficerNameController, LoCheckYourAnswersController, RemoveLiaisonOfficerController}
import forms.YesNoAnswerFormProvider
import models.journeydata.liaisonofficers.{LiaisonOfficer, LiaisonOfficerCommunication}
import models.{NormalMode, YesNoAnswer}
import org.jsoup.Jsoup
import play.api.data.Form
import play.api.test.Helpers.running
import viewmodels.checkAnswers.liaisonofficers.{AddedLiaisonOfficerSummary, AddedLiaisonOfficersViewModel}

class AddedLiaisonOfficersViewModelSpec extends SpecBase {

  private val formProvider            = new YesNoAnswerFormProvider()
  private val form: Form[YesNoAnswer] = formProvider("addedLiaisonOfficers.error.required")

  private val inProgressOfficer =
    LiaisonOfficer(
      id = "in-progress-id",
      fullName = Some("Jane Smith"),
      email = Some("jane@example.com"),
      phoneNumber = None,
      communication = Set(LiaisonOfficerCommunication.ByEmail)
    )

  private val completeOfficer =
    LiaisonOfficer(
      id = "complete-id",
      fullName = Some("John Doe"),
      email = Some("john@example.com"),
      phoneNumber = Some("07987654321"),
      communication = Set(LiaisonOfficerCommunication.ByPhone)
    )

  "AddedLiaisonOfficersViewModel" - {

    "must render complete and in progress sections when both are present" in {

      running(app) {
        val viewModel = app.injector.instanceOf[AddedLiaisonOfficersViewModel]

        val html = viewModel(
          form = form,
          summary = AddedLiaisonOfficerSummary(Seq(inProgressOfficer), Seq(completeOfficer))
        )(messages(app))

        val doc = Jsoup.parse(html.toString)

        doc.text() must include(messages(app)("addedLiaisonOfficers.complete"))
        doc.text() must include(messages(app)("addedLiaisonOfficers.inProgress"))
        doc.text() must include("John Doe")
        doc.text() must include("Jane Smith")
      }
    }

    "must not render the complete heading when only complete officers are present" in {

      running(app) {
        val viewModel = app.injector.instanceOf[AddedLiaisonOfficersViewModel]

        val html = viewModel(
          form = form,
          summary = AddedLiaisonOfficerSummary(Seq.empty, Seq(completeOfficer))
        )(messages(app))

        val doc = Jsoup.parse(html.toString)

        doc.text() must not include messages(app)("addedLiaisonOfficers.complete")
        doc.text() must include("John Doe")
      }
    }

    "must render the in progress heading when in progress officers are present" in {

      running(app) {
        val viewModel = app.injector.instanceOf[AddedLiaisonOfficersViewModel]

        val html = viewModel(
          form = form,
          summary = AddedLiaisonOfficerSummary(Seq(inProgressOfficer), Seq.empty)
        )(messages(app))

        val doc = Jsoup.parse(html.toString)

        doc.text() must include(messages(app)("addedLiaisonOfficers.inProgress"))
        doc.text() must include("Jane Smith")
      }
    }

    "must render correct change and remove links for incomplete and complete liaison officers" in {

      running(app) {
        val viewModel = app.injector.instanceOf[AddedLiaisonOfficersViewModel]

        val html = viewModel(
          form = form,
          summary = AddedLiaisonOfficerSummary(Seq(inProgressOfficer), Seq(completeOfficer))
        )(messages(app))

        val doc   = Jsoup.parse(html.toString)
        val links = doc.select("a").eachAttr("href")

        links must contain(LiaisonOfficerNameController.onPageLoad(Some(inProgressOfficer.id), NormalMode).url)
        links must contain(RemoveLiaisonOfficerController.onPageLoad(inProgressOfficer.id).url)
        links must contain(LoCheckYourAnswersController.onPageLoad(completeOfficer.id).url)
        links must contain(RemoveLiaisonOfficerController.onPageLoad(completeOfficer.id).url)
      }
    }

    "must render the yes/no radios when the number of liaison officers is below the maximum" in {

      running(app) {
        val viewModel = app.injector.instanceOf[AddedLiaisonOfficersViewModel]

        val html = viewModel(
          form = form,
          summary = AddedLiaisonOfficerSummary(Seq(inProgressOfficer), Seq.empty)
        )(messages(app))

        val doc = Jsoup.parse(html.toString)

        doc.text() must include(messages(app)("addedLiaisonOfficers.legend"))
        doc.select("input[type=radio][name=value]").size() mustEqual 2
      }
    }

    "must not render the yes/no radios when the number of liaison officers is equal to the maximum" in {

      running(app) {
        val appConfig = app.injector.instanceOf[FrontendAppConfig]
        val viewModel = app.injector.instanceOf[AddedLiaisonOfficersViewModel]

        val complete =
          (1 to appConfig.maxLiaisonOfficers).map { i =>
            LiaisonOfficer(
              id = s"id-$i",
              fullName = Some(s"Officer $i"),
              email = Some(s"officer$i@example.com"),
              phoneNumber = Some(s"0700000000$i"),
              communication = Set(LiaisonOfficerCommunication.ByEmail)
            )
          }

        val html = viewModel(
          form = form,
          summary = AddedLiaisonOfficerSummary(Seq.empty, complete)
        )(messages(app))

        val doc = Jsoup.parse(html.toString)

        doc.text() must not include messages(app)("addedLiaisonOfficers.legend")
        doc.select("input[type=radio][name=value]").size() mustEqual 0
      }
    }

    "must not render the yes/no radios when the number of liaison officers is above the maximum" in {

      running(app) {
        val appConfig = app.injector.instanceOf[FrontendAppConfig]
        val viewModel = app.injector.instanceOf[AddedLiaisonOfficersViewModel]

        val complete =
          (1 to (appConfig.maxLiaisonOfficers + 1)).map { i =>
            LiaisonOfficer(
              id = s"id-$i",
              fullName = Some(s"Officer $i"),
              email = Some(s"officer$i@example.com"),
              phoneNumber = Some(s"0700000000$i"),
              communication = Set(LiaisonOfficerCommunication.ByEmail)
            )
          }

        val html = viewModel(
          form = form,
          summary = AddedLiaisonOfficerSummary(Seq.empty, complete)
        )(messages(app))

        val doc = Jsoup.parse(html.toString)

        doc.text() must not include messages(app)("addedLiaisonOfficers.legend")
        doc.select("input[type=radio][name=value]").size() mustEqual 0
      }
    }
  }
}
