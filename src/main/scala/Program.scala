package hello

import cats.{ Monad }
import cats.implicits._

sealed abstract class Foo // ADT
final object Seed0 extends Foo
final object Seed1 extends Foo

// Custom Program
sealed abstract class Program[F[_]: Monad] { self =>
  def run[A](in: A): F[A]
  def runme[A](in: A): List[A] = Program.ListFooProgram.run(in)
}

object Program {
  def apply[F[_]](implicit F: Program[F]): Program[F] = F

  implicit val ListFooProgram = new Program[List] {
    def run[Foo](in: Foo) = List(in)
  }

}
