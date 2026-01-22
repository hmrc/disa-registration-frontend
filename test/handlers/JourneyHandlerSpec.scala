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

package handlers

import base.SpecBase
import models.journeydata.TaskListSection
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import pages.PageWithDependents

class JourneyHandlerSpec extends SpecBase {

  final case class TestSection(value: String) extends TaskListSection {
    override def sectionName: String = "test"
  }

  "JourneyHandler.clearStalePages" - {

    "return updated unchanged when pagesToClear is empty" in {
      val changedPage = mock[PageWithDependents[TestSection]]
      val existing    = TestSection("before")
      val updated     = TestSection("after")

      when(changedPage.pagesToClear(existing, updated)).thenReturn(Nil)

      val result = JourneyHandler.clearStalePages(changedPage, existing, updated)

      result shouldBe updated
      verify(changedPage).pagesToClear(existing, updated)
    }

    "apply clearAnswer for each page returned by pagesToClear" in {
      val changedPage = mock[PageWithDependents[TestSection]]
      val page1       = mock[PageWithDependents[TestSection]]
      val page2       = mock[PageWithDependents[TestSection]]

      val existing = TestSection("before")
      val updated  = TestSection("after")

      val afterPage1 = TestSection("after-1")
      val afterPage2 = TestSection("after-2")

      when(changedPage.pagesToClear(existing, updated)).thenReturn(List(page1, page2))

      when(page1.clearAnswer(updated)).thenReturn(afterPage1)
      when(page2.clearAnswer(afterPage1)).thenReturn(afterPage2)

      val result = JourneyHandler.clearStalePages(changedPage, existing, updated)

      result shouldBe afterPage2

      verify(changedPage).pagesToClear(existing, updated)
      verify(page1).clearAnswer(updated)
      verify(page2).clearAnswer(afterPage1)
    }

    "work with a single page to clear" in {
      val changedPage = mock[PageWithDependents[TestSection]]
      val page1       = mock[PageWithDependents[TestSection]]

      val existing = TestSection("before")
      val updated  = TestSection("after")

      val cleared = TestSection("cleared")

      when(changedPage.pagesToClear(existing, updated)).thenReturn(List(page1))
      when(page1.clearAnswer(updated)).thenReturn(cleared)

      val result = JourneyHandler.clearStalePages(changedPage, existing, updated)

      result shouldBe cleared
      verify(page1).clearAnswer(updated)
    }
  }
}
