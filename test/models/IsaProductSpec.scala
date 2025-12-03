package models

import generators.ModelGenerators
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class IsaProductSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaCheckPropertyChecks
    with OptionValues
    with ModelGenerators {

  "IsaProduct" - {

    "must deserialise valid values" in {

      val gen = arbitrary[IsaProduct]

      forAll(gen) { isaProducts =>
        JsString(isaProducts.toString).validate[IsaProduct].asOpt.value mustEqual isaProducts
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!IsaProduct.values.map(_.toString).contains(_))

      forAll(gen) { invalidValue =>
        JsString(invalidValue).validate[IsaProduct] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = arbitrary[IsaProduct]

      forAll(gen) { isaProducts =>
        Json.toJson(isaProducts) mustEqual JsString(isaProducts.toString)
      }
    }
  }
}
