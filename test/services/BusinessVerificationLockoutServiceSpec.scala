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
import repositories.BusinessVerificationLockoutRepository
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.any

import scala.concurrent.Future

class BusinessVerificationLockoutServiceSpec extends SpecBase {

  private val service = new BusinessVerificationLockoutService(mockBvLockoutRepository)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockBvLockoutRepository)
  }

  "BusinessVerificationLockoutService" - {

    "lockUserOut should call repository.lockUser" in {
      when(mockBvLockoutRepository.lockUser(any[String]))
        .thenReturn(Future.successful(()))

      val result = service.lockUserOut("user-1").futureValue

      result mustBe (())
      verify(mockBvLockoutRepository).lockUser("user-1")
    }

    "isUserLockedOut should return true when repository returns true" in {
      when(mockBvLockoutRepository.isLockedOut("user-1"))
        .thenReturn(Future.successful(true))

      val result = service.isUserLockedOut("user-1").futureValue

      result mustBe true
      verify(mockBvLockoutRepository).isLockedOut("user-1")
    }

    "isUserLockedOut should return false when repository returns false" in {
      when(mockBvLockoutRepository.isLockedOut("user-2"))
        .thenReturn(Future.successful(false))

      val result = service.isUserLockedOut("user-2").futureValue

      result mustBe false
      verify(mockBvLockoutRepository).isLockedOut("user-2")
    }
  }
}
