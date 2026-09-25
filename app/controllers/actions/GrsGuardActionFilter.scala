package controllers.actions

import models.journeydata.BusinessVerification
import models.requests.OptionalDataRequest
import navigation.Navigator
import play.api.mvc.{ActionFilter, AnyContent, Result, Results}
import play.api.mvc.Results.{Ok, Redirect}
import play.twirl.api.HtmlFormat

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GrsGuardActionFilter @Inject()(
                                navigator: Navigator
                              ) (implicit val executionContext: ExecutionContext)
  extends ActionFilter[OptionalDataRequest] with Results {

  override protected def filter[A](request: OptionalDataRequest[A]): Future[Option[Result]] = {
    val businessV = request.journeyData.flatMap(_.businessVerification)

    val isVerified = businessV match {
      case Some(business) => checkVerificationAndRegistration(business)
      case _ => false
    }

    val result = if isVerified then None else Some(Redirect(navigator.nextPageGrsGuard()))
    Future.successful(result)

  }

  private def checkVerificationAndRegistration(business: BusinessVerification): Boolean = {
    (for {
      verification <- business.businessVerificationPassed
      registration <- business.businessRegistrationPassed
    } yield {
      verification && registration
    }).getOrElse(false)
  }
}
