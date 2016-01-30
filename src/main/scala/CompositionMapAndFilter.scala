
import rx.lang.scala._
import scala.io._
import scala.concurrent.duration._

object CompositionMapAndFilter extends App {
 
  val odds = Observable.interval(0.5.seconds)
    .filter(_ % 4 == 0).scan((0l, 1l, 0l)) { (acc, ev) => (acc._1+ev / acc._2 , acc._2 + 1, ev)}
    //.filter(_ % 2 == 1).map(n=> s"num $n").scan(0) { (
    odds.subscribe(println _ , e => println(s"unexpected $e"), () => println("No more odds" ))
        
  
    
    
  Thread.sleep(30000)
    
  
  
  
}