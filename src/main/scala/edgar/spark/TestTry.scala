package edgar.spark

import scala.util.Try
import scala.util.Success
import scala.util.Failure
import scala.util.Random

object TestTry extends App{
  val rand = new Random()
  def attemptOpen(url:String):Try[String] = {
    
    def open(url:String) = {
      val nxt = rand.nextInt(10) 
      if (nxt % 2 == 0) url
      else throw new IllegalArgumentException(s"Exceptionfoo:$nxt")
    }
    
    Try(open(url))
  }
  
  val res = attemptOpen("myurl") match {
    case Success(c) => c
    case Failure(ex) => ex.getMessage()
  }
 
  println(res)
  
}