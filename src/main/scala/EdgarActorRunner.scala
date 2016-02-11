

import akka.actor._

import edgar.actors._
import edgar.actors.EdgarRequests._
import edgar.ftp._

import java.util.UUID

object EdgarActorRunner extends App {

  println("Starting the Actor System....")

  val system = ActorSystem("Edgar-Filings-Downloader")

  val ftpClient = new ApacheFTPClient {
      val ftpConfig = new FtpConfig {
        val username = "anonymous"
        val password = UUID.randomUUID().toString() + "@downloader.com"
        val host = "ftp.sec.gov"
      }
    }

    

  //val downloader = system.actorOf(Props(classOf[Downloader], ftpClient), "Downloader")
  
  val downloader =
    system.actorOf(Props(classOf[DownloadManager], 6), "DownloadManager")
  val edgarFileSink = system.actorOf(Props[EdgarFileSink], "EdgarFileSink")
  val edgarFileManager = system.actorOf(Props(classOf[EdgarFileManager],
    downloader, edgarFileSink), "EdgarFileManager")
  
  val indexProcessor = system.actorOf(Props(classOf[IndexProcessor],
    edgarFileManager), "IndexProcessor")

  val indexRetriever = system.actorOf(Props(classOf[IndexRetriever],
    indexProcessor, downloader , ftpClient, "edgar/daily-index"), "IndexRetriever")
    
    
  val master = system.actorOf(Props(classOf[EdgarMaster],
    indexRetriever, downloader,
    indexProcessor,
    edgarFileManager), "Master")

  master ! Start

}