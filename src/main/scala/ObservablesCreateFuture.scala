

/**
 * @author marco

 */

import scala.concurrent._
import ExecutionContext.Implicits.global
import rx.lang.scala._
import scala.io._
import scala.concurrent.duration._

object ObservablesCreateFuture extends App {
  
  def loadURLContent(url:String):Iterator[String] = {
    Source.fromURL(url).getLines
  }
  
  def createObservableFromListOfItems() = {
    val linesList:Iterable[String] = loadURLContent("http://www.google.com").toIterable
    val o = Observable.from(linesList)
    o.subscribe(item => println(s"We got $item"))
  }
  
  def createObservableFromFuture() = {
    val future = Future {
                    loadURLContent("http://www.google.com")
    }
    
    future foreach {
      case lines => lines.foreach(line => println(s"got:$line")) 
    }
    
    
    val mappedFut = future map {
      case lines => lines.map(line => line.size)
    }
    
    mappedFut foreach {
      case sizes => sizes.foreach(println(_))
    }
    
    
    Thread.sleep(5000)
    
    
  }
  
  createObservableFromFuture
  
  
  /**
  def fetchUrlFuture():Future[String] = {
  
   Future {
      blocking {
        Source.fromURL("http://www.google.com").mkString("\n`")
        
      }
    }
  }
  //val o = Observable.from(fetchUrlFuture)
  **/
  
  
  /**
   * 
   * 
   * val f = Future {"Bakc to the future"}
     val o = Observable.create[String] ( 
                  obs => f foreach { 
                        case s => obs.onNext(s);obs.onCompleted()
                         }
              f.failed foreach {case t => obs.onError(t)}
         Subscription()
      }
      * **/
      //createObservableFromListOfItems
      
}