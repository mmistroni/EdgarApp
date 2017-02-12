package edgar.actors

import akka.actor._
import akka.actor.SupervisorStrategy.{ Resume, Escalate, Restart }
import akka.event.Logging

import edgar.util.LogHelper
import edgar.core.EdgarTypes
import edgar.core.EdgarFiling

object EdgarRequests extends LogHelper {

    import EdgarTypes.{ SimpleFiling, XBRLFiling }

    type SimpleFiling = String

    sealed trait EdgarRequest

    sealed trait EdgarResponse

    case object Start

    case object DownloadLatestIndex extends EdgarRequest

    case class FileIndex(fileName: String) extends EdgarRequest

    case class ProcessIndexFile(fileContent: String) extends EdgarRequest

    case class DownloadFile(filePath: String) extends EdgarRequest

    case class DownloadXBRLFile(filePath: String) extends EdgarRequest

    case class FilteredFiles(files: Seq[EdgarFiling]) extends EdgarRequest

    case class FileContent(content: String) extends EdgarResponse

    case class StreamContent(xbrl: XBRLFiling) extends EdgarResponse

    case class FilingInfo(xmlContent: SimpleFiling) extends EdgarResponse

    case class FilingXBRLInfo(contentList: XBRLFiling) extends EdgarResponse

    case class SNSMessage(content:String) extends EdgarResponse
    
    case class EdgarAggregatorMessage(content:Map[String, String]) extends EdgarResponse
    
    
    case object Shutdown

    def createEdgarFilingMessage(filing: EdgarFiling): EdgarRequest = filing.formType match {
      case xbrl if List("10-K", "20-F", "40-F", "10-Q", 
                        "8-K", 
                        "6-K").contains(xbrl) => {
        val filePath = filing.filingPath;
        val adjustedPath = filePath.substring(0, filePath.indexOf(".")).replace("-", "")
        val fileName = filePath.split("/").last.replace(".txt", "-xbrl.zip")
        val fullPath = s"$adjustedPath/$fileName"
        logger.info("Creting Message:" + filing)
        DownloadFile(fullPath)
      }
      case _ => DownloadFile(filing.filingPath)
    }

  }
