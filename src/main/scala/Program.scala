package hello


import cats.{Monad}

sealed abstract class Program[F[_]:Monad] {
  def run[A]:F[A]
}

object Program {
  
}