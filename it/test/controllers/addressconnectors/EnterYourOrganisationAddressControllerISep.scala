package controllers.addressconnectors

import models.journeydata.CorrespondenceAddress
import org.jsoup.Jsoup
import play.api.http.Status.*
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, POST, contentAsString, route, status, writeableOf_AnyContentAsEmpty, writeableOf_AnyContentAsFormUrlEncoded}
import uk.gov.hmrc.http.SessionKeys
import utils.WiremockHelper.{stubGet, stubPost}
import utils.{BaseIntegrationSpec, CommonStubs, WiremockHelper}

class EnterYourOrganisationAddressControllerISep  extends BaseIntegrationSpec with CommonStubs with WiremockHelper  {

  private val controllerEndpoint = s"$registrationFrontendRoutePrefix/enter-your-organisation-address"
  private val getJourneyDataUrl = s"/disa-registration/store/$testGroupId"
  private val addressUrl = "/address-lookup/lookup"
  val testSectionAnswers = CorrespondenceAddress(addressLine1 = Some("1 Test Street"),postCode = Some("AB1 2CD"),uprn = Some("100023336956"))
  val updateTaskListJourneyUrl = s"/disa-registration/store/$testGroupId/organisationDetails"

  val emailJourneyData: String =
    s"""
       |{
       |  "groupId": "$testGroupId",
       |  "enrolmentId": "$testString",
       |  "businessVerification": {
       |    "businessRegistrationPassed": true,
       |    "businessVerificationPassed": true,
       |    "ctutr": "1234567890"
       |  },
       |  "organisationDetails": {
       |    "registeredToManageIsa": "no",
       |    "tradingUsingDifferentName": "no",
       |    "fcaNumber": "123456",
       |    "registeredAddressCorrespondence": "no",
       |    "correspondenceAddress": {
       |      "addressLine1": "bhoooo",
       |      "postCode": "AB1 23CD"
       |    }
       |  }
       |}
       |""".stripMargin

  "GET /enter-your-organisation-address" should {

    "return 200 and show the correct page" in {
      stubAuth()
      stubGet(getJourneyDataUrl, OK, validGrsJourneyData)

      val request =
        FakeRequest(GET, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get
      val title = Jsoup.parse(contentAsString(result)).title

      status(result) shouldBe OK
      title should include("Enter your organisation’s address")
    }

    "return 303 when no Journey data is available" in {
      stubAuth()

      val request =
        FakeRequest(GET, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
    }
  }

  "POST /enter-your-organisation-address" should {

    "return 303 when an address is submitted" in {
      stubAuth()
      //Stub GRS
      stubGet(getJourneyDataUrl, OK, emailJourneyData)
      //Stub Connector
      stubPost(
        addressUrl,
        OK,
        """[{"uprn": "100023336956", "addressLine1": "1 Test Street", "postCode": "AB1 2CD"}]"""
      )
      //Stub taskList update
      stubPost(updateTaskListJourneyUrl, NO_CONTENT, "")

      val request =
        FakeRequest(POST, controllerEndpoint)
          .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")
          .withFormUrlEncodedBody("addressLine1" -> "1 Test Street",
            "postcode"     -> "AB1 2CD")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
    }

  }
}
