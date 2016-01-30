

/**
 * @author marco

 */

import scala.concurrent._
import ExecutionContext.Implicits.global
import rx.lang.scala._
import scala.io._
import scala.concurrent.duration._

object ObservablesExercise2 extends App {
  
    
  val evens =  Observable.interval(5.second).filter(_ % 6 != 0)
  
  val odds = Observable.interval(12.second).filter(_ % 5 != 0)
  
  val merged = evens.merge(odds)
  
  merged.subscribe(it => println(it))
  
  Thread.sleep(60000)
       
}