package views

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest

trait ViewSpecBase extends AnyWordSpec with GuiceOneAppPerSuite with Matchers with MockitoSugar with ScalaFutures {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val request: Request[AnyContent] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("en")))
  val messagesApi: MessagesApi = app.injector.instanceOf[play.api.i18n.MessagesApi]
}
