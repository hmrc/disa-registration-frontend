package pages

import pages.PageConstants.organisationDetailsSection
import play.api.libs.json.JsPath

object RegisteredIsaManagerPage extends QuestionPage[Boolean] {
  override def path: JsPath = organisationDetailsSection \ toString

  override def toString: String = "registeredIsaManager"
}
