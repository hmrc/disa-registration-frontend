package controllers.orgdetails

import controllers.actions.Actions
import forms.{YesNoFormProvider, ZReferenceNumberFormProvider}
import pages.{RegisteredIsaManagerPage, ZReferenceNumberPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.orgdetails.ZReferenceNumberView

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

  def onPageLoad(): Action[AnyContent] = actions.requireData().async { implicit request =>
    val preparedForm = request.userAnswers.get(RegisteredIsaManagerPage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

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
