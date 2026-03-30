package controllers.grs

import base.SpecBase
import models.journeydata.{BusinessVerification, RegisteredAddress}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import scala.concurrent.Future

class GrsControllerSpec extends SpecBase {

  val journeyId = "testJourneyId"

  private def fakeRequest =
    FakeRequest(GET, controllers.routes.GrsController.grsCallback(journeyId).url)

  private def bv(
                  regPassed: Option[Boolean],
                  verPassed: Option[Boolean]
                ) = BusinessVerification(
    businessVerificationPassed = verPassed,
    businessRegistrationPassed = regPassed,
    ctUtr = Some("1234567890"),
    registeredAddress = Some(
      RegisteredAddress(
        addressLine1 = Some("line1"),
        addressLine2 = Some("line2"),
        addressLine3 = Some("line3"),
        postCode = Some("AA1 1AA")
      )
    )
  )

  "GrsController" - {

    "must redirect to TaskList when both registration and verification pass" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      val verification = bv(Some(true), Some(true))

      when(
        mockGrsOrchestrationService.processGrsJourney(
          eqTo(journeyId),
          any(),
          any[String],
          any[String]
        )(any())
      ).thenReturn(Future.successful(verification))

      running(application) {
        val result = route(application, fakeRequest).value

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe controllers.routes.TaskListController.onPageLoad().url
      }
    }

    "must redirect to Lockout when business verification fails" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      val verification = bv(Some(true), Some(false))

      when(
        mockGrsOrchestrationService.processGrsJourney(
          eqTo(journeyId),
          any(),
          any[String],
          any[String]
        )(any())
      ).thenReturn(Future.successful(verification))

      running(application) {
        val result = route(application, fakeRequest).value

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe controllers.routes.BusinessVerificationController.lockout().url
      }
    }

    "must redirect to lockout when both BV and registration fail" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      val verification = bv(Some(false), Some(false))

      when(
        mockGrsOrchestrationService.processGrsJourney(
          eqTo(journeyId),
          any(),
          any[String],
          any[String]
        )(any())
      ).thenReturn(Future.successful(verification))

      running(application) {
        val result = route(application, fakeRequest).value

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe controllers.routes.BusinessVerificationController.lockout().url
      }
    }

    "must redirect to Start when registration fails and verification is empty" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      val verification = bv(None, None)

      when(
        mockGrsOrchestrationService.processGrsJourney(
          eqTo(journeyId),
          any(),
          any[String],
          any[String]
        )(any())
      ).thenReturn(Future.successful(verification))

      running(application) {
        val result = route(application, fakeRequest).value

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe controllers.routes.StartController.onPageLoad().url
      }
    }

    "must propagate exception if orchestration service fails (e.g. address lookup failure)" in {

      val application = applicationBuilder(journeyData = Some(emptyJourneyData)).build()

      when(
        mockGrsOrchestrationService.processGrsJourney(
          eqTo(journeyId),
          any(),
          any[String],
          any[String]
        )(any())
      ).thenReturn(Future.failed(new RuntimeException("Address lookup failed")))

      running(application) {
        val request = fakeRequest
        val thrown  = route(application, request).value.failed.futureValue

        thrown.getMessage shouldBe "Address lookup failed"
      }
    }
  }
}