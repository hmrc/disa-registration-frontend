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

package controllers.orgdetails

import controllers.actions.Actions
import forms.YesNoFormProvider
import pages.RegisteredIsaManagerPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.orgdetails.RegisteredIsaManagerView

import javax.inject.Inject
import scala.concurrent.Future

class RegisteredIsaManagerController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  formProvider: YesNoFormProvider,
  view: RegisteredIsaManagerView
) extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider("orgDetails.registeredIsaManager.error.missing")

  def onPageLoad(): Action[AnyContent] = actions.getData().async { implicit request =>
    val preparedForm = request.userAnswers.fold(form)(_.get(RegisteredIsaManagerPage) match {
      case None        => form
      case Some(value) => form.fill(value)
    })

    Future.successful(Ok(view(preparedForm)))
  }

  def onSubmit(): Action[AnyContent] = actions.identify().async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        value => Future.successful(NotFound)
      )
  }
}
