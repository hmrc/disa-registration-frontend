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

package viewmodels

import base.SpecBase
import config.FrontendAppConfig
import controllers.signatories.routes.{RemoveSignatoryController, SignatoryNameController}
import models.CheckMode
import models.journeydata.signatories.Signatory
import org.jsoup.Jsoup
import play.api.data.Form
import play.api.test.Helpers.*

class AddedSignatoriesViewModelSpec extends SpecBase {

  private val formProvider = new forms.YesNoAnswerFormProvider()
  private val form: Form[_] = formProvider("addedSignatory.error.required")

  private val inProgressSignatory =
    Signatory(
      id = "1",
      fullName = Some("Jane Smith"),
      jobTitle = None
    )

  private val completeSignatory =
    Signatory(
      id = "2",
      fullName = Some("John Doe"),
      jobTitle = Some("Director")
    )

  private val unnamedSignatory =
    Signatory(
      id = "3",
      fullName = None,
      jobTitle = Some("Director")
    )

  "AddedSignatoriesViewModel" - {

    "must render both in-progress and complete sections when present" in {

      running(app) {
        val viewModel = app.injector.instanceOf[AddedSignatoriesViewModel]

        val html = viewModel(
          form,
          Seq(inProgressSignatory),
          Seq(completeSignatory)
        )(messages(app))

        val doc = Jsoup.parse(html.toString)

        doc.text() must include(messages(app)("addedSignatory.complete"))
        doc.text() must include(messages(app)("addedSignatory.inProgress"))
        doc.text() must include("Jane Smith")
        doc.text() must include("John Doe")
      }
    }

    "must not render the 'complete' section heading when inProgress is empty" in {

      running(app) {
        val viewModel = app.injector.instanceOf[AddedSignatoriesViewModel]

        val html = viewModel(
          form,
          Seq.empty,
          Seq(completeSignatory)
        )(messages(app))

        val doc = Jsoup.parse(html.toString)

        doc.text() must include("John Doe")
        doc.text() must not include messages(app)("addedSignatory.complete")
        doc.text() must not include messages(app)("addedSignatory.inProgress")
      }
    }

    "must render only in-progress section when complete is empty" in {

      running(app) {
        val viewModel = app.injector.instanceOf[AddedSignatoriesViewModel]

        val html = viewModel(
          form,
          Seq(inProgressSignatory),
          Seq.empty
        )(messages(app))

        val doc = Jsoup.parse(html.toString)

        doc.text() must include("Jane Smith")
        doc.text() must include(messages(app)("addedSignatory.inProgress"))
        doc.text() must not include messages(app)("addedSignatory.complete")
      }
    }

    "must not render a row when fullName is missing" in {

      running(app) {
        val viewModel = app.injector.instanceOf[AddedSignatoriesViewModel]

        val html = viewModel(
          form,
          Seq(unnamedSignatory),
          Seq.empty
        )(messages(app))

        val doc = Jsoup.parse(html.toString)

        doc.text() must not include "Director"
      }
    }

    "must render change and remove links correctly" in {

      running(app) {
        val viewModel = app.injector.instanceOf[AddedSignatoriesViewModel]

        val html = viewModel(
          form,
          Seq(inProgressSignatory),
          Seq(completeSignatory)
        )(messages(app))

        val doc = Jsoup.parse(html.toString)
        val links = doc.select("a").eachAttr("href")

        links must contain(SignatoryNameController.onPageLoad(Some("1"), CheckMode).url)
        links must contain(RemoveSignatoryController.onPageLoad("1").url)
        links must contain(SignatoryNameController.onPageLoad(Some("2"), CheckMode).url)
        links must contain(RemoveSignatoryController.onPageLoad("2").url)
      }
    }

    "must render yes/no radios when below max" in {

      running(app) {
        val viewModel = app.injector.instanceOf[AddedSignatoriesViewModel]

        val html = viewModel(
          form,
          Seq(inProgressSignatory),
          Seq.empty
        )(messages(app))

        val doc = Jsoup.parse(html.toString)

        doc.text() must include(messages(app)("addedSignatory.addAnother"))
        doc.select("input[type=radio][name=value]").size() mustEqual 2
      }
    }

    "must not render radios when at max signatories" in {

      running(app) {
        val appConfig = app.injector.instanceOf[FrontendAppConfig]
        val viewModel = app.injector.instanceOf[AddedSignatoriesViewModel]

        val complete =
          (1 to appConfig.maxSignatories).map { i =>
            Signatory(i.toString, Some(s"Officer $i"), Some("Director"))
          }

        val html = viewModel(
          form,
          Seq.empty,
          complete
        )(messages(app))

        val doc = Jsoup.parse(html.toString)

        doc.text() must not include messages(app)("addedSignatory.addAnother")
        doc.select("input[type=radio][name=value]").size() mustEqual 0
      }
    }

    "must not render radios when above max signatories" in {

      running(app) {
        val appConfig = app.injector.instanceOf[FrontendAppConfig]
        val viewModel = app.injector.instanceOf[AddedSignatoriesViewModel]

        val complete =
          (1 to (appConfig.maxSignatories + 1)).map { i =>
            Signatory(i.toString, Some(s"Officer $i"), Some("Director"))
          }

        val html = viewModel(
          form,
          Seq.empty,
          complete
        )(messages(app))

        val doc = Jsoup.parse(html.toString)

        doc.text() must not include messages(app)("addedSignatory.addAnother")
        doc.select("input[type=radio][name=value]").size() mustEqual 0
      }
    }
  }
}