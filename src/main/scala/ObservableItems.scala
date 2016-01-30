

/**
 * @author marco

 */

import rx.lang.scala._
import java.lang.Thread.State

object ObservableItems extends App {
  
  val o  = Observable.items("Pascal", "Java", "Scala")
  
  val thread = new Thread("my custom thread") {
          override def run {
            //println("Thread running..")
          }
  }
  
  
  val threadObservable = Observable.create[Thread] {obs =>
     
     if (Thread.activeCount() > 0) {
         obs.onNext(Thread.currentThread());
         obs.onCompleted()
     }
     Subscription()
  }
  
  
  threadObservable.subscribe(event=> println("Thread started"))
      
  

}

