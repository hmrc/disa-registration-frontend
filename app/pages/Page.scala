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

package pages

import models.journeydata.TaskListSection

import scala.language.implicitConversions

trait Page[A <: TaskListSection] {
  def clearAnswer(sectionAnswers: A): A
}

trait PageWithDependents[A <: TaskListSection] extends Page[A] {
  def pagesToClear(before: A, after: A): List[Page[A]]

  def resumeNormalMode(before: A, after: A): Boolean
}

object Page {

  implicit def toString[A <: TaskListSection](page: Page[A]): String =
    page.toString
}
