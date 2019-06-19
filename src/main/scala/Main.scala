package hello

import scalaz.zio.{App, UIO}
//import scalaz.zio.console.{putStrLn}

object Main extends App {

  def run(args: List[String]) =
    res.fold(_ => 1, _ => 0)
  

    val res = UIO.succeed(0)

  /* val res:UIO[A] =
    for {
      data <- Program[F]

    } yield() */
  
}
