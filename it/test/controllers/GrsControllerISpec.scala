package controllers

import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.SessionKeys
import utils.WiremockHelper.{stubGet, stubPost}
import utils.{BaseIntegrationSpec, CommonStubs}

class GrsControllerISpec extends BaseIntegrationSpec with CommonStubs {

  private val journeyId = "test-journey-id"

  private val callbackUrl =
    s"/obligations/enrolment/isa/incorporated-identity-callback?journeyId=$journeyId"

  private val getJourneyDataUrl =
    s"/disa-registration/store/$testGroupId/businessVerification"

  private val fetchGrsJourneyUrl =
    s"/incorporated-entity-identification/api/journey/$journeyId"

  private val addressLookupUrl = "/lookup"

  private def request =
    FakeRequest(GET, callbackUrl)
      .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token")

  private val baseJourneyData =
    s"""
       |{
       |  "groupId": "$testGroupId"
       |}
       |""".stripMargin

  // --------------------------
  // SUCCESS PATHS
  // --------------------------

  "GET /incorporated-identity-callback" should {

    "redirect to TaskList when registration and verification both pass (with address enrichment)" in {

      val grsResponse =
        """
          |{
          |  "companyProfile": {
          |    "companyName": "Test Company Ltd",
          |    "companyNumber": "01234567"
          |  },
          |  "identifiersMatch": true,
          |  "registration": {
          |    "registrationStatus": "REGISTERED"
          |  },
          |  "businessVerification": {
          |    "verificationStatus": "PASS"
          |  },
          |  "registeredAddress": {
          |    "addressLine1": "1 Test Street",
          |    "postCode": "AA1 1AA"
          |  }
          |}
          |""".stripMargin

      val addressLookupResponse =
        """
          |{
          |  "addresses": [
          |    { "uprn": "123456789" }
          |  ]
          |}
          |""".stripMargin

      stubAuth()
      stubGet(getJourneyDataUrl, OK, baseJourneyData)
      stubGet(fetchGrsJourneyUrl, OK, grsResponse)
      stubPost(addressLookupUrl, OK, addressLookupResponse)
      stubPost(getJourneyDataUrl, NO_CONTENT, "")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe
        Some(controllers.routes.TaskListController.onPageLoad().url)
    }

    "use default UPRN when address lookup returns no results" in {

      val grsResponse =
        """
          |{
          |  "companyProfile": {
          |    "companyName": "Test Company Ltd",
          |    "companyNumber": "01234567"
          |  },
          |  "identifiersMatch": true,
          |  "registration": {
          |    "registrationStatus": "REGISTERED"
          |  },
          |  "businessVerification": {
          |    "verificationStatus": "PASS"
          |  },
          |  "registeredAddress": {
          |    "addressLine1": "1 Test Street",
          |    "postCode": "AA1 1AA"
          |  }
          |}
          |""".stripMargin

      val emptyLookupResponse =
        """
          |{
          |  "addresses": []
          |}
          |""".stripMargin

      stubAuth()
      stubGet(getJourneyDataUrl, OK, baseJourneyData)
      stubGet(fetchGrsJourneyUrl, OK, grsResponse)
      stubPost(addressLookupUrl, OK, emptyLookupResponse)
      stubPost(getJourneyDataUrl, NO_CONTENT, "")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
    }

    // --------------------------
    // CONTROLLER BRANCHING
    // --------------------------

    "redirect to lockout when verification fails" in {

      val grsResponse =
        """
          |{
          |  "registration": { "registrationStatus": "REGISTERED" },
          |  "businessVerification": { "verificationStatus": "FAIL" }
          |}
          |""".stripMargin

      stubAuth()
      stubGet(getJourneyDataUrl, OK, baseJourneyData)
      stubGet(fetchGrsJourneyUrl, OK, grsResponse)
      stubPost(getJourneyDataUrl, NO_CONTENT, "")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe
        Some(controllers.routes.BusinessVerificationController.lockout().url)
    }

    "redirect to start when registration fails" in {

      val grsResponse =
        """
          |{
          |  "registration": { "registrationStatus": "REGISTRATION_FAILED" }
          |}
          |""".stripMargin

      stubAuth()
      stubGet(getJourneyDataUrl, OK, baseJourneyData)
      stubGet(fetchGrsJourneyUrl, OK, grsResponse)
      stubPost(getJourneyDataUrl, NO_CONTENT, "")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe
        Some(controllers.routes.StartController.onPageLoad().url)
    }

    "redirect to start when verification not present" in {

      val grsResponse =
        """
          |{
          |  "registration": { "registrationStatus": "REGISTERED" }
          |}
          |""".stripMargin

      stubAuth()
      stubGet(getJourneyDataUrl, OK, baseJourneyData)
      stubGet(fetchGrsJourneyUrl, OK, grsResponse)
      stubPost(getJourneyDataUrl, NO_CONTENT, "")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
    }

    // --------------------------
    // FAILURE SCENARIOS
    // --------------------------

    "return 500 when address lookup fails" in {

      val grsResponse =
        """
          |{
          |  "registration": { "registrationStatus": "REGISTERED" },
          |  "businessVerification": { "verificationStatus": "PASS" },
          |  "registeredAddress": {
          |    "addressLine1": "1 Test Street",
          |    "postCode": "AA1 1AA"
          |  }
          |}
          |""".stripMargin

      stubAuth()
      stubGet(getJourneyDataUrl, OK, baseJourneyData)
      stubGet(fetchGrsJourneyUrl, OK, grsResponse)
      stubPost(addressLookupUrl, INTERNAL_SERVER_ERROR, "")

      val result = route(app, request).get

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return 500 when GRS passes but no address returned" in {

      val grsResponse =
        """
          |{
          |  "registration": { "registrationStatus": "REGISTERED" },
          |  "businessVerification": { "verificationStatus": "PASS" }
          |}
          |""".stripMargin

      stubAuth()
      stubGet(getJourneyDataUrl, OK, baseJourneyData)
      stubGet(fetchGrsJourneyUrl, OK, grsResponse)

      val result = route(app, request).get

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return 500 when journey update fails" in {

      val grsResponse =
        """
          |{
          |  "registration": { "registrationStatus": "REGISTERED" },
          |  "businessVerification": { "verificationStatus": "PASS" },
          |  "registeredAddress": {
          |    "addressLine1": "1 Test Street",
          |    "postCode": "AA1 1AA"
          |  }
          |}
          |""".stripMargin

      val addressLookupResponse =
        """{ "addresses": [ { "uprn": "123" } ] }"""

      stubAuth()
      stubGet(getJourneyDataUrl, OK, baseJourneyData)
      stubGet(fetchGrsJourneyUrl, OK, grsResponse)
      stubPost(addressLookupUrl, OK, addressLookupResponse)
      stubPost(getJourneyDataUrl, INTERNAL_SERVER_ERROR, "")

      val result = route(app, request).get

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return 500 when GRS call fails" in {

      stubAuth()
      stubGet(getJourneyDataUrl, OK, baseJourneyData)
      stubGet(fetchGrsJourneyUrl, INTERNAL_SERVER_ERROR, "")

      val result = route(app, request).get

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    // --------------------------
    // EDGE CASES
    // --------------------------

    "handle missing journey data (404)" in {

      val grsResponse =
        """
          |{
          |  "registration": { "registrationStatus": "REGISTERED" },
          |  "businessVerification": { "verificationStatus": "FAIL" }
          |}
          |""".stripMargin

      stubAuth()
      stubGet(getJourneyDataUrl, NOT_FOUND, "")
      stubGet(fetchGrsJourneyUrl, OK, grsResponse)
      stubPost(getJourneyDataUrl, NO_CONTENT, "")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
    }

    "ignore identifiersMatch=false and still process" in {

      val grsResponse =
        """
          |{
          |  "identifiersMatch": false,
          |  "registration": { "registrationStatus": "REGISTERED" },
          |  "businessVerification": { "verificationStatus": "PASS" },
          |  "registeredAddress": {
          |    "addressLine1": "1 Test Street",
          |    "postCode": "AA1 1AA"
          |  }
          |}
          |""".stripMargin

      val addressLookupResponse =
        """{ "addresses": [ { "uprn": "123" } ] }"""

      stubAuth()
      stubGet(getJourneyDataUrl, OK, baseJourneyData)
      stubGet(fetchGrsJourneyUrl, OK, grsResponse)
      stubPost(addressLookupUrl, OK, addressLookupResponse)
      stubPost(getJourneyDataUrl, NO_CONTENT, "")

      val result = route(app, request).get

      status(result) shouldBe SEE_OTHER
    }
  }
}