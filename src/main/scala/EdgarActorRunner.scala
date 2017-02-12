

import akka.actor._

import edgar.actors._
import edgar.actors.EdgarRequests._
import edgar.predicates.EdgarPredicates._
import edgar.ftp._
import edgar.core._
import edgar.email._
import edgar.predicates.EdgarPredicates.or
import java.util.UUID
import com.typesafe.config._
import amazon.util.AWSClientFactory
    
object EdgarActorRunner extends App with edgar.util.LogHelper {

  logger.info("Starting the Actor System....")
  val filterFunction = formType2In(Seq("13F-HR"))
  val factory = EdgarFactory
    
  def createFilterFunction():EdgarFilter = {
    val cikFilter = cikIn(Set("886982", "19617", "1067983"))     // GS, JPM. BRKB)
    val includeFormTypesFilter = formTypeIn(Set("4"))
    val excludeFormTypesFilter = excludeFormTypes(List("424B2", "8-K"))
    val sameCikFilter  = and(Seq(cikFilter, excludeFormTypesFilter))_
    //or(Seq(sameCikFilter, includeFormTypesFilter ))_
    includeFormTypesFilter
    
  }
  
  
  def launchActorSystem(appConfig:Config) = {
    
    
    val system = ActorSystem("Edgar-Filings-Downloader")
    val downloader =
      system.actorOf(Props(classOf[DownloadManager], 3, factory), "DownloadManager")
     
    println("Recipients:" + appConfig.getString("smtp.recipients"))
      
    val emailSink = new S3Sink(appConfig)
    
    val edgarFileSink = system.actorOf(Props(classOf[EdgarFileSinkActor], 
        emailSink
        ), "EdgarFileSink")
    val edgarFileManager = system.actorOf(Props(classOf[EdgarFileManager],
      downloader, edgarFileSink), "EdgarFileManager")
  
    val indexProcessor = system.actorOf(Props(classOf[IndexProcessorActor],
      new IndexProcessorImpl(filterFunction),
      edgarFileManager), "IndexProcessor")
  
    val indexRetriever = system.actorOf(Props(classOf[IndexRetrieverActor],
      indexProcessor, downloader, factory.edgarFtpClient("https://www.sec.gov/Archives/" ),
      "edgar/daily-index/"), "IndexRetriever")
  
    val master = system.actorOf(Props(classOf[EdgarMaster],
      indexRetriever, downloader,
      indexProcessor,
      edgarFileManager), "Master")

    logger.info("Scheduling timeout...")
    import scala.concurrent.duration.Duration;
    import java.util.concurrent.TimeUnit;
    // Creating Timer
    system.scheduler.scheduleOnce(
        Duration.create(3, TimeUnit.HOURS), 
        edgarFileSink, Terminated)( system.dispatcher)
      
    master ! Start
  }
  
  // load sys properties required for email sink
  val conf = ConfigFactory.load()
  
  
  
  launchActorSystem(conf)
  

}