

import akka.actor._

import edgar.actors._
import edgar.actors.EdgarRequests._
import edgar.ftp._
import edgar.core._

import java.util.UUID

object EdgarActorRunner extends App with LogHelper {

  logger.info("Starting the Actor System....")

  val system = ActorSystem("Edgar-Filings-Downloader")

  val factory = EdgarFactory

  /**
   * Simple text filings
   *
   * <documentType>4</documentType>
   *
   * <periodOfReport>2016-02-25</periodOfReport>
   *
   * <issuer>
   * <issuerCik>0001000623</issuerCik>
   *
   *
   *
   * XBRL filings
   * -<xbrli:context id="FD2015Q4YTD">
   *
   *
   * -<xbrli:entity>
   *
   * <xbrli:identifier scheme="http://www.sec.gov/CIK">0001000623</xbrli:identifier>
   *
   * </xbrli:entity>
   *
   *
   * -<xbrli:period>
   *
   * <xbrli:startDate>2015-01-01</xbrli:startDate>
   *
   * <xbrli:endDate>2015-12-31</xbrli:endDate>
   *
   * </xbrli:period>
   *
   * </xbrli:context>
   *
   *
   *
   */

  val downloader =
    system.actorOf(Props(classOf[DownloadManager], 3, factory), "DownloadManager")
  val edgarFileSink = system.actorOf(Props(classOf[EdgarFileSinkActor], factory.edgarSink()), "EdgarFileSink")
  val edgarFileManager = system.actorOf(Props(classOf[EdgarFileManager],
    downloader, edgarFileSink), "EdgarFileManager")

  val filterFunction = (lineArray: Array[String]) => lineArray.size > 2 && lineArray(2) == "13-F"
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