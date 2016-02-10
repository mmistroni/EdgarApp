

import akka.actor._

import edgar.core._
import edgar.actors._

import java.util.UUID

object EdgarActorRunner extends App {

  println("Starting the Actor System....")

  val system = ActorSystem("Edgar-Filings-Downloader")

  

  //val downloader = system.actorOf(Props(classOf[Downloader], ftpClient), "Downloader")
  
  val downloader =
    system.actorOf(Props(classOf[DownloadManager], 10), "DownloadManager")
  val indexRetriever = system.actorOf(Props(classOf[IndexRetriever],
    null, "edgar/daily-index"), "IndexRetriever")
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