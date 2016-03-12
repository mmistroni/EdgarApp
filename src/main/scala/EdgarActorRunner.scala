

import akka.actor._

import edgar.actors._
import edgar.actors.EdgarRequests._
import edgar.predicates.EdgarPredicates._
import edgar.ftp._
import edgar.core._
import edgar.predicates.EdgarPredicates.or
import java.util.UUID

object EdgarActorRunner extends App with LogHelper {

  logger.info("Starting the Actor System....")
  
  def createFilterFunction():EdgarFilter = {
    val cikFilter = cikIn(Set("886982", "19617", "1067983"))     // GS, JPM. BRKB)
    val includeFormTypesFilter = formTypeIn(Set("13F-HR"))
    val excludeFormTypesFilter = excludeFormTypes(List("424B2"))
    val sameCikFilter  = and(Seq(cikFilter, excludeFormTypesFilter))_
    or(Seq(sameCikFilter, includeFormTypesFilter ))_
  }
  
  val filterFunction = createFilterFunction() //formType2In(Seq("13F-HR"))
  
  val system = ActorSystem("Edgar-Filings-Downloader")
  val factory = EdgarFactory

  
  val downloader =
    system.actorOf(Props(classOf[DownloadManager], 3, factory), "DownloadManager")
  val edgarFileSink = system.actorOf(Props(classOf[EdgarFileSinkActor], factory.edgarSink()), "EdgarFileSink")
  val edgarFileManager = system.actorOf(Props(classOf[EdgarFileManager],
    downloader, edgarFileSink), "EdgarFileManager")

  val indexProcessor = system.actorOf(Props(classOf[IndexProcessorActor],
    new IndexProcessorImpl(filterFunction),
    edgarFileManager), "IndexProcessor")

  val indexRetriever = system.actorOf(Props(classOf[IndexRetrieverActor],
    indexProcessor, downloader, factory.edgarFtpClient(UUID.randomUUID().toString() + "@downloader.com"),
    "edgar/daily-index"), "IndexRetriever")

  val master = system.actorOf(Props(classOf[EdgarMaster],
    indexRetriever, downloader,
    indexProcessor,
    edgarFileManager), "Master")

  master ! Start

}