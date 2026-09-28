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

package controllers.actions

import models.journeydata.OrganisationDetails
import models.journeydata.certificatesofauthority.CertificatesOfAuthority
import models.journeydata.isaproducts.IsaProducts
import models.journeydata.liaisonofficers.LiaisonOfficers
import models.journeydata.signatories.Signatories
import models.journeydata.thirdparty.{ThirdParty, ThirdPartyOrganisations}
import models.requests.DataRequest
import navigation.Navigator
import play.api.mvc.{ActionFilter, Call, Result, Results}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CyaGuardActionFilter @Inject() (navigator: Navigator)(implicit ec: ExecutionContext) {

  def apply(isComplete: Boolean): ActionFilter[DataRequest] =
    new ActionFilter[DataRequest] {
      override protected def executionContext: ExecutionContext = ec

      override protected def filter[A](request: DataRequest[A]): Future[Option[Result]] =
        Future.successful(
          if isComplete then None
          else Some(Results.Redirect(navigator.nextPageCyaGuard()))
        )
    }

  def sortingData[T](data: T, id: Option[String] = None): Option[Call] = {
    val isThereData = data match
      case Some(organisation: OrganisationDetails) => true
      case Some(product: IsaProducts)              => true
      case Some(coa: CertificatesOfAuthority)      => true
      case Some(lo: LiaisonOfficers)               => id.exists(i => lo.liaisonOfficers.exists(o => o.id == i && !o.inProgress))
      case Some(s: Signatories)                    => id.exists(i => s.signatories.exists(x => x.id == i && !x.inProgress))
      case Some(parties: ThirdPartyOrganisations)  => true // !parties.thirdParties.find(_.id == id.get).get.inProgress
      case Some(t: ThirdParty)                     => if !t.inProgress then true else false
      case _                                       => false

    if isThereData then None else Some(navigator.nextPageCyaGuard())
  }
}
