import scala.concurrent._

import ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Try, Success, Failure }
import scala.util.{ Success, Failure }
import scala.io._
import scala.xml.XML

import edgar.ftp._

/**
object FutureEdgarRunner extends App {
  println("Kicking off EdgarModule")

  import edgar.core.EdgarFactory
  val factory = EdgarFactory
  
  val edgarClient = factory.edgarFtpClient("tmp@gmail.com")

  // Future 1. Retrieve All Filng Files
  def getFilingFiles(filingFiles: List[(String, String, String)]): Future[List[String]] = Future {

    val xmlContents = for ((cik, form, filingFile) <- filingFiles) yield edgarClient.retrieveFile(filingFile)

    xmlContents.map(content => content.substring(content.indexOf("<ownershipDocument>"), content.indexOf("</XML")))

  }

  // Future 2. Get All Files in MasterDir
  def getEgarMasterIndexesFuture(baseDirName: String, edgarClient: FtpClient): Future[List[String]] = Future {
    edgarClient.listDirectory(baseDirName)
  }

  def extractContent(fileContent: String): List[String] = {
    fileContent.split("\n").toList
  }

  // Future 3. process the List of files
  def processList(dirContent: List[String], edgarClient: FtpClient): Future[List[String]] = Future {
    val latest = dirContent.last
    edgarClient.retrieveFile(s"edgar/daily-index/$latest").split("\n").toList
  }

  // Future4 . filter lines  we are interested in
  def filterLines(fileLines: List[String]): Future[List[Array[String]]] = Future {
    fileLines.filter(line => line.split('|').size > 2 && line.split('|')(2) == "4").map(ln => ln.split('|'))
  }

  val allFilesFuture = getEgarMasterIndexesFuture("edgar/daily-index", edgarClient)
    .flatMap { listOfFiles => processList(listOfFiles, edgarClient) }
    .flatMap { lines => filterLines(lines) }

  allFilesFuture onSuccess {
    case lines => {
      val tpls = lines.map(lnArr => (lnArr(0), lnArr(2), lnArr(4)))
      val contentFut = getFilingFiles(tpls.take(10))
      contentFut onSuccess {
        case xmlContents => { println(xmlContents) }
      }
    }

  }

  Thread.sleep(30000)

}
**/