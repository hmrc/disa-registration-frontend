package handlers
import org.scalatestplus.play.*
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.*
import play.api.mvc.*
import play.api.test.*
import play.api.i18n.*
import play.api.test.Helpers.await
import play.api.test.Helpers.defaultAwaitTimeout
import play.twirl.api.Html
import views.html.{ErrorTemplate, NotFoundView}

import scala.concurrent.ExecutionContext.Implicits.global

class ErrorHandlerSpec extends PlaySpec with MockitoSugar {

  val messagesApi: MessagesApi     = mock[MessagesApi]
  val errorTemplate: ErrorTemplate = mock[ErrorTemplate]
  val notFoundView: NotFoundView   = mock[NotFoundView]

  implicit val request: RequestHeader = FakeRequest()
  implicit val messages: Messages     = MessagesImpl(Lang.defaultLang, messagesApi)

  "ErrorHandler" should {

    "render not found template correctly" in {

      val errorHandler = new ErrorHandler(messagesApi, errorTemplate, notFoundView)

      val expectedHtml = Html("<h1>Page Not Found</h1>")

      when(notFoundView.apply()(any[RequestHeader], any[Messages])).thenReturn(expectedHtml)

      val result = errorHandler.notFoundTemplate
      val html   = await(result)
      html.body must include("Page Not Found")

    }
  }
}
