
import scala.concurrent._
import ExecutionContext.Implicits.global
import rx.lang.scala._
import scala.io._
import scala.concurrent.duration._

object CompositionConcatAndFlatten extends App {

  def fetchQuote():Future[String] = Future {
    blocking {
      val url = "http://api.theysaidso.com/qod.xml"
        //"http://www.iheartquotes.com/api/v1/random?" + "show_permalink=false&show_source=false"
      Source.fromURL(url).getLines.toString
    }
  }
  
  def fetchQuotesObservable():Observable[String] = {
    Observable.from(fetchQuote())
  }
  
  def quotes:Observable[Observable[String]] = 
    Observable.interval(0.5 seconds).take(4).map {
        n => fetchQuotesObservable().map(txt => s"$n) $txt")
    }
  
  
  println("Using concat..")
  quotes.concat.subscribe(item => println (item))
  println("Using flatten..")
  quotes.flatten.subscribe(item => println (item))
  
  
  Thread.sleep(20000)
  
  
}

  
  
