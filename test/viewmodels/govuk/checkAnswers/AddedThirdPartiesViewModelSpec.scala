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

package viewmodels.govuk.checkAnswers

import base.SpecBase
import config.FrontendAppConfig
import controllers.thirdparty.routes.*
import forms.YesNoAnswerFormProvider
import models.NormalMode
import models.journeydata.thirdparty.ThirdParty
import org.scalatest.matchers.should.Matchers.should
import viewmodels.checkAnswers.thirdparty.{AddedThirdPartiesSummary, AddedThirdPartiesViewModel}

class AddedThirdPartiesViewModelSpec extends SpecBase {

  private val tp1 = ThirdParty("1", Some("Org 1"))
  private val tp2 = ThirdParty("2", Some("Org 2"))

  "AddedThirdPartiesSummary" - {

    "must return singular keys when only one item" in {
      val summary = AddedThirdPartiesSummary(Seq(tp1), Nil, 10)

      summary.count mustEqual 1
      summary.multiple mustBe false
      summary.titleKey mustEqual "addedThirdParties.title"
    }

    "must return plural keys when multiple items" in {
      val summary = AddedThirdPartiesSummary(Seq(tp1), Seq(tp2), 10)

      summary.count mustEqual 2
      summary.multiple mustBe true
      summary.titleKey mustEqual "addedThirdParties.title.plural"
    }
  }

  "AddedThirdPartiesViewModel" - {

    "must render complete section when complete items exist" in {

      val vm   = app.injector.instanceOf[AddedThirdPartiesViewModel]
      val form = new YesNoAnswerFormProvider()("error.required")

      val html = vm(form, Nil, Seq(tp1))

      html.toString must not include (messages("addedThirdParties.complete"))
      html.toString must include("Org 1")
    }

    "must render in-progress section when items exist" in {

      val vm   = app.injector.instanceOf[AddedThirdPartiesViewModel]
      val form = new YesNoAnswerFormProvider()("error.required")

      val html = vm(form, Seq(tp1), Nil)

      html.toString must include(messages("addedThirdParties.inProgress"))
    }

    "must show radios when below max" in {

      val vm   = app.injector.instanceOf[AddedThirdPartiesViewModel]
      val form = new YesNoAnswerFormProvider()("error.required")

      val html = vm(form, Seq(tp1), Nil)
      html.toString must include(messages("addedThirdParties.addAnother"))
    }

    "must NOT show radios when at max" in {

      val vm   = app.injector.instanceOf[AddedThirdPartiesViewModel]
      val form = new YesNoAnswerFormProvider()("addedThirdParties.error.required")

      val max   = app.injector.instanceOf[FrontendAppConfig].maxThirdParties
      val items = (1 to max).map(i => ThirdParty(i.toString, Some(s"Org $i")))

      val html = vm(form, items, Nil)
      html.toString must not include (messages("addedThirdParties.addAnother"))
    }

    "must link in-progress items to edit page" in {

      val vm   = app.injector.instanceOf[AddedThirdPartiesViewModel]
      val form = new YesNoAnswerFormProvider()("error.required")

      val html = vm(form, Seq(tp1), Nil)

      html.toString must include(
        ThirdPartyOrgDetailsController.onPageLoad(Some("1"), NormalMode).url
      )
    }

    "must link complete items to check answers page" in {

      val vm   = app.injector.instanceOf[AddedThirdPartiesViewModel]
      val form = new YesNoAnswerFormProvider()("error.required")

      val html = vm(form, Nil, Seq(tp1))

      html.toString must include(
        ThirdPartyCheckYourAnswersController.onPageLoad("1").url
      )
    }
  }
}
