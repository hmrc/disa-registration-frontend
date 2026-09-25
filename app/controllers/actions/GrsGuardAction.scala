package controllers.actions

import models.journeydata.BusinessVerification
import models.requests.OptionalDataRequest
import navigation.Navigator
import play.api.mvc.{AnyContent, Result}
import play.api.mvc.Results.{Ok, Redirect}
import play.twirl.api.HtmlFormat

import javax.inject.Inject

class GrsGuardAction @Inject()(
                                navigator: Navigator
                              ) {

  def businessVerificationGuard(request: OptionalDataRequest[AnyContent], page: HtmlFormat.Appendable): Result = {
    val businessV = request.journeyData.flatMap(_.businessVerification)

    val result = businessV match {
      case Some(business) => checkVerificationAndRegistration(business)
      case _ => false
    }

    if result then Ok(page) else Redirect(navigator.nextPageGrsGuard())

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
