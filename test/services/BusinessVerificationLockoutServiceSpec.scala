/*
 * Copyright 2026 HM Revenue & Customs
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

package services

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import repositories.BusinessVerificationLockoutRepository

import scala.concurrent.Future

class BusinessVerificationLockoutServiceSpec extends SpecBase {

  private val service = new BusinessVerificationLockoutService(mockBvLockoutRepository)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockBvLockoutRepository)
  }

  "BusinessVerificationLockoutService" - {

    "lockout should call repository.lockOrg" in {
      when(mockBvLockoutRepository.lockOrg(any[String], any[String]))
        .thenReturn(Future.successful(()))

      val result = service.lockout("group-1", "ctutr-1").futureValue

      result mustBe (())
      verify(mockBvLockoutRepository).lockOrg("group-1", "ctutr-1")
    }

    "isGroupLockedOut should return true when repository returns true" in {
      when(mockBvLockoutRepository.isGroupLockedOut("group-1"))
        .thenReturn(Future.successful(true))

      val result = service.isGroupLockedOut("group-1").futureValue

      result mustBe true
      verify(mockBvLockoutRepository).isGroupLockedOut("group-1")
    }

    "isGroupLockedOut should return false when repository returns false" in {
      when(mockBvLockoutRepository.isGroupLockedOut("group-2"))
        .thenReturn(Future.successful(false))

      val result = service.isGroupLockedOut("group-2").futureValue

      result mustBe false
      verify(mockBvLockoutRepository).isGroupLockedOut("group-2")
    }
  }
}
