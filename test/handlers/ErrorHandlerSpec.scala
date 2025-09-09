package handlers

import play.twirl.api.Html
import views.ViewSpecBase
import views.html.{ErrorTemplate, NotFoundView}

class ErrorHandlerSpec extends ViewSpecBase {

  private val errorTemplate = app.injector.instanceOf[ErrorTemplate]
  private val notFoundView  = app.injector.instanceOf[NotFoundView]

  private val errorHandler = new ErrorHandler(messagesApi, errorTemplate, notFoundView)

  "ErrorHandler" should {

    "render not found template correctly" in {
      val result = errorHandler.notFoundTemplate

      whenReady(result) { html =>
        val content = html.body

        content must include(messages("pageNotFound.heading"))
        content must include(messages("pageNotFound.paragraph1"))
        content must include(messages("pageNotFound.paragraph2"))
      }
    }
  }
}
