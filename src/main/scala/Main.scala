package hello

import scalaz.zio.{App, UIO, Task}
import scala.collection.parallel.Task
//import scalaz.zio.console.{putStrLn}

object Main extends App {

  def run(args: List[String]) =
    res1.fold(_ => 1, _ => 0)
  
  // Succeed dummy
  val res0 = UIO.succeed(0)  
  
  // Succeed with the value of program run 
  val prog:List[List[Foo]] = Program[List].runme(List(Seed0, Seed1))  
  val res1 = Task.succeed(prog)
  
}
