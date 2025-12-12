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

package config

import base.SpecBase

class FrontendAppConfigSpec extends SpecBase {

  val appConfig = app.injector.instanceOf[FrontendAppConfig]

  "FrontendAppConfig" - {

    "must generate DISA backend URL" in {
      appConfig.disaRegistrationBaseUrl mustBe "http://localhost:1201"
    }

    "must return p2pLoansInformation url" in {
      appConfig.p2pLoansInformationUrl mustBe "https://www.gov.uk/government/consultations/isa-qualifying-investments-consultation-on-including-peer-to-peer-loans/isa-qualifying-investments-consultation-on-including-peer-to-peer-loans#:~:text=Peer%2Dto%2Dpeer%20loans%20are,terms%20agreed%20between%20the%20parties."
    }
  }
}
