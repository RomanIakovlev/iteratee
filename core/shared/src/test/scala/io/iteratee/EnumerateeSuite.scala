package io.iteratee

import cats.{ Eval, Monad }
import org.scalacheck.{ Gen, Prop }

abstract class EnumerateeSuite[F[_]: Monad] extends ModuleSuite[F] {
  test("map") {
    check { (eav: EnumeratorAndValues[Int]) =>
      eav.enumerator.mapE(map(_ + 1)).drain === F.pure(eav.values.map(_ + 1))
    }
  }

  test("flatMap") {
    check { (eav: EnumeratorAndValues[Int]) =>
      val enumerator = eav.enumerator.mapE(flatMap(v => enumVector(Vector(v, v))))

      enumerator.drain === F.pure(eav.values.flatMap(v => Vector(v, v)))
    }
  }

  test("collect") {
    check { (eav: EnumeratorAndValues[Int]) =>
      val pf: PartialFunction[Int, Int] = {
        case v if v % 2 == 0 => v + 1
      }

      eav.enumerator.mapE(collect(pf)).drain === F.pure(eav.values.collect(pf))
    }
  }

  test("filter") {
    check { (eav: EnumeratorAndValues[Int]) =>
      val p: Int => Boolean = _ % 2 == 0

      eav.enumerator.mapE(filter(p)).drain === F.pure(eav.values.filter(p))
    }
  }

  test("sequenceI") {
    check { (eav: EnumeratorAndValues[Int]) =>
      Prop.forAll(Gen.posNum[Int]) { n =>
        eav.enumerator.mapE(sequenceI(take(n))).drain === F.pure(eav.values.grouped(n).toVector)
      }
    }
  }

  test("uniq") {
    check { (xs: Vector[Int]) =>
      val sorted = xs.sorted

      enumVector(sorted).mapE(uniq).drain === F.pure(sorted.distinct)
    }
  }

  test("zipWithIndex") {
    check { (eav: EnumeratorAndValues[Int]) =>
      val result = eav.values.zipWithIndex.map {
        case (v, i) => (v, i.toLong)
      }

      eav.enumerator.mapE(zipWithIndex).drain === F.pure(result)
    }
  }

  test("grouped") {
    check { (eav: EnumeratorAndValues[Int]) =>
      Prop.forAll(Gen.posNum[Int]) { n =>
        eav.enumerator.mapE(grouped(n)).drain === F.pure(eav.values.grouped(n).toVector)
      }
    }
  }

  test("splitOn") {
    check { (eav: EnumeratorAndValues[Int]) =>
      val p: Int => Boolean = _ % 2 == 0

      def splitOnEvens(xs: Vector[Int]): Vector[Vector[Int]] = if (xs.isEmpty) Vector.empty else {
        val (before, after) = xs.span(x => !p(x))

        before +: splitOnEvens(after.drop(1))
      }

      eav.enumerator.mapE(splitOn(p)).drain === F.pure(splitOnEvens(eav.values))
    }
  }

  test("cross") {
    check { (eav1: EnumeratorAndValues[Int], eav2: EnumeratorAndValues[Int]) =>
      val result = for {
        v1 <- eav1.values
        v2 <- eav2.values
      } yield (v1, v2)

      eav1.enumerator.mapE(cross(eav2.enumerator)).drain === F.pure(result)
    }
  }
}

class EvalEnumerateeTests extends EnumerateeSuite[Eval] with EvalSuite
