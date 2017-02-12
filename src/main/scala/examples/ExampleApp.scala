package examples

/**
 * this is a sample application that shows how to fetch
 * and process Edgar Files.
 * It's a 'singlethread' version of the EdgarActorRunner 
 */
import akka.actor._

import edgar.actors.EdgarRequests._
import edgar.predicates.EdgarPredicates._
import edgar.ftp
import edgar.core._
import edgar.util.LogHelper
import edgar.predicates.EdgarPredicates.or
import java.util.UUID


object ExampleApp extends App with LogHelper {
  import edgar.core.EdgarFactory
  
  val filterFunction = formType2In(Seq("4"))  // filtering form 4
  val indexDir = "https://www.sec.gov/Archives/" //edgar/daily-index/"
  val indexProcessor = new IndexProcessorImpl(filterFunction)
  
  def getFtpClient() = {
    EdgarFactory.edgarFtpClient(indexDir)
  }
  
  def getIndexFile() = {
    // Picking latest edgar index File
    val latestEdgarFileName = getFtpClient().listDirectory("edgar/daily-index/").last
    logger.info("Latest fileName is:" + latestEdgarFileName)
    // download it
    getFtpClient().retrieveFile(s"$latestEdgarFileName")
  }
  
  def processIndexFile(indexFileContent:String) = {
    logger.info("Processign Form4 filings")
    // process It . for simplicity, we only get one of the filings
    indexProcessor.processIndexFile(indexFile).head
  }
  
  val indexFile = getIndexFile()
  val form4EdgarFiling = processIndexFile(indexFile)
  
  // retrieving it
  logger.info(s"Retrieving form4 filings from ${form4EdgarFiling.filingPath}")
  logger.info(getFtpClient().retrieveFile(form4EdgarFiling.filingPath))
  
  
}