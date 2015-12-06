package io.iteratee

import algebra.Eq
import cats.{ Eval, Monad }
import cats.std.AllInstances
import cats.syntax.AllSyntax
import org.scalatest.FunSuite
import org.scalatest.prop.Checkers
import org.typelevel.discipline.scalatest.Discipline

class BaseSuite extends FunSuite with Checkers with Discipline
  with ArbitraryInstances with EqInstances
  with AllInstances with AllSyntax {
  override def convertToEqualizer[T](left: T): Equalizer[T] = ???
}

abstract class ModuleSuite[F[_]](implicit val F: Monad[F]) extends BaseSuite
  with Module[F] with ArbitraryEnumerators[F] {
  def monadName: String

  implicit def eqF[A: Eq]: Eq[F[A]]
}

trait EvalSuite { this: ModuleSuite[Eval] =>
  def monadName: String = "Eval"

  implicit def eqF[A: Eq]: Eq[Eval[A]] = Eval.evalEq
}
