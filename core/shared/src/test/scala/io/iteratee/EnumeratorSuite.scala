package io.iteratee

import algebra.Eq
import algebra.laws.GroupLaws
import cats.{ Eval, Monad }
import cats.laws.discipline.MonadTests
import org.scalacheck.{ Gen, Prop }

abstract class EnumeratorSuite[F[_]: Monad] extends ModuleSuite[F] {
  type EnumeratorF[E] = Enumerator[F, E]

  checkAll(s"Enumerator[$monadName, Int]", GroupLaws[Enumerator[F, Int]].monoid)
  checkAll(s"Enumerator[$monadName, Int]", MonadTests[EnumeratorF].monad[Int, Int, Int])

  test("liftM") {
    check { (i: Int) =>
      liftToEnumerator(F.pure(i)).drain === F.pure(Vector(i))
    }
  }

  test("empty") {
    empty[Int].drain === F.pure(Vector.empty)
  }

  test("enumEnd") {
    check { (eav: EnumeratorAndValues[Int]) =>
      eav.enumerator.append(enumEnd[Int]).append(eav.enumerator).drain === F.pure(eav.values)
    }
  }

  test("enumOne") {
    check { (i: Int) =>
      enumOne(i).drain === F.pure(Vector(i))
    }
  }

  test("enumStream") {
    check { (xs: Stream[Int]) =>
      enumStream(xs).drain === F.pure(xs.toVector)
    }
  }

  test("enumList") {
    check { (xs: List[Int]) =>
      enumList(xs).drain === F.pure(xs.toVector)
    }
  }

  test("enumVector") {
    check { (xs: Vector[Int]) =>
      enumVector(xs).drain === F.pure(xs)
    }
  }

  test("enumIndexedSeq") {
    check { (xs: Vector[Int], start: Int, count: Int) =>
      enumIndexedSeq(xs, start, start + count).drain === F.pure(xs.slice(start, start + count))
    }
  }

  test("repeat") {
    check { (i: Int, count: Short) =>
      repeat(i).fold(take(count.toInt)) === F.pure(Vector.fill(count.toInt)(i))
    }
  }

  test("iterate") {
    check { (i: Int, count: Short) =>
      iterate(i)(_ + 1).fold(take(count.toInt)) === F.pure(Vector.iterate(i, count.toInt)(_ + 1))
    }
  }

  test("drain") {
    check { (eav: EnumeratorAndValues[Int]) =>
      eav.enumerator.drain === F.pure(eav.values)
    }
  }

  test("drainTo") {
    check { (eav: EnumeratorAndValues[Int]) =>
      eav.enumerator.drainTo[List] === F.pure(eav.values.toList)
    }
  }

  test("prepend") {
    check { (eav: EnumeratorAndValues[Int], v: Int) =>
      eav.enumerator.prepend(v).drain === F.pure(v +: eav.values)
    }
  }

  test("bindM") {
    check { (eav: EnumeratorAndValues[Int]) =>
      val enumeratorF: F[Option[Enumerator[F, String]]] =
        eav.enumerator.bindM(v => Option(enumOne(v.toString)))

      enumeratorF.map(_.map(_.drain)) === F.pure(Option(F.pure(eav.values.map(_.toString))))
    }
  }

  test("intoEnumerator") {
    import syntax._

    check { (i: Int) =>
      F.pure(i).intoEnumerator.drain === F.pure(Vector(i))
    }
  }

  test("flatten") {
    check { (v: Int) =>
      enumOne(F.pure(v)).flatten[Int].drain === F.pure(Vector(v))
    }
  }

  test("reduced") {
    check { (eav: EnumeratorAndValues[Int]) =>
      eav.enumerator.reduced(Vector.empty[Int])(_ :+ _).drain === F.pure(Vector(eav.values))
    }
  }

  test("map") {
    check { (eav: EnumeratorAndValues[Int]) =>
      eav.enumerator.map(_ + 1).drain === F.pure(eav.values.map(_ + 1))
    }
  }

  test("flatMap") {
    check { (eav: EnumeratorAndValues[Int]) =>
      val enumerator = eav.enumerator.flatMap(v => enumVector(Vector(v, v)))

      enumerator.drain === F.pure(eav.values.flatMap(v => Vector(v, v)))
    }
  }

  test("collect") {
    check { (eav: EnumeratorAndValues[Int]) =>
      val pf: PartialFunction[Int, Int] = {
        case v if v % 2 == 0 => v + 1
      }

      eav.enumerator.collect(pf).drain === F.pure(eav.values.collect(pf))
    }
  }

  test("filter") {
    check { (eav: EnumeratorAndValues[Int]) =>
      val p: Int => Boolean = _ % 2 == 0

      eav.enumerator.filter(p).drain === F.pure(eav.values.filter(p))
    }
  }

  test("sequenceI") {
    check { (eav: EnumeratorAndValues[Int]) =>
      Prop.forAll(Gen.posNum[Int]) { n =>
        eav.enumerator.sequenceI(take(n)).drain === F.pure(eav.values.grouped(n).toVector)
      }
    }
  }

  test("uniq") {
    check { (xs: Vector[Int]) =>
      val sorted = xs.sorted

      enumVector(sorted).uniq.drain === F.pure(sorted.distinct)
    }
  }

  test("zipWithIndex") {
    check { (eav: EnumeratorAndValues[Int]) =>
      val result = eav.values.zipWithIndex.map {
        case (v, i) => (v, i.toLong)
      }

      eav.enumerator.zipWithIndex.drain === F.pure(result)
    }
  }

  test("grouped") {
    check { (eav: EnumeratorAndValues[Int]) =>
      Prop.forAll(Gen.posNum[Int]) { n =>
        eav.enumerator.grouped(n).drain === F.pure(eav.values.grouped(n).toVector)
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

      eav.enumerator.splitOn(p).drain === F.pure(splitOnEvens(eav.values))
    }
  }

  test("cross") {
    check { (eav1: EnumeratorAndValues[Int], eav2: EnumeratorAndValues[Int]) =>
      val result = for {
        v1 <- eav1.values
        v2 <- eav2.values
      } yield (v1, v2)

      eav1.enumerator.cross(eav2.enumerator).drain === F.pure(result)
    }
  }
}

class EvalEnumeratorTests extends EnumeratorSuite[Eval] with EvalSuite {
  test("perform") {
    check { (eav: EnumeratorAndValues[Int]) =>
      var counter = 0
      val action = perform[Int](Eval.always(counter += 1))
      val enumerator = action.append(eav.enumerator).append(action)

      counter === 0 && enumerator.drain === F.pure(eav.values) && counter === 2
    }
  }
}
