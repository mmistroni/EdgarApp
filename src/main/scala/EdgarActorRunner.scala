

import akka.actor._
import edgar.core._
import edgar.actors._

object EdgarActorRunner extends App {

  println("Starting the Actor System....")

  val system = ActorSystem("Edgar-Filings-Downloader")
  
  val edgarClient = new EdgarModule {
    val ftpClient = new ApacheFTPClient {
      val ftpConfig = new FtpConfig {
        val username = "anonymous"
        val password = "tmp2@gmail.com"
        val host = "ftp.sec.gov"
      }
    }
  }
  

  val downloader = system.actorOf(Props(classOf[Downloader], edgarClient), "Downloader")
  val indexRetriever = system.actorOf(Props(classOf[IndexRetriever],
        edgarClient, "edgar/daily-index"), "IndexRetriever")
  val edgarFileSink = system.actorOf(Props[EdgarFileSink], "EdgarFileSink")
  val edgarFileManager = system.actorOf(Props(classOf[EdgarFileManager],
    downloader, edgarFileSink), "EdgarFileManager")
  val indexProcessor = system.actorOf(Props(classOf[IndexProcessor],
    edgarFileManager), "IndexProcessor")

  val master = system.actorOf(Props(classOf[EdgarMaster],
    indexRetriever, downloader,
    indexProcessor,
    edgarFileManager), "Master")

  master ! Start

}