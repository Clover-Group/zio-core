package hello

import cats.{Monad}
import cats.implicits._

sealed abstract class Foo // ADT

// Monadic class
sealed abstract class Program[F[_]:Monad] { self => 
  def run[A](in:A):F[A]
}

object Program {
  def apply[F[_]](implicit F:Program[F]):Program[F] = F

  implicit val ListIntProgram = new Program[List] {
    def run[Int](in:Int):List[Int] = List(in)
  }      

  implicit val ListFooProgram = new Program[List] {
    def run[Foo](in:Foo) = List(in) 
  }      

}