
import akka.actor._
import edgar.core._
import scala.xml.XML
import akka.event.Logging

package edgar.actors {

  sealed trait EdgarRequest

  case object Start

  case object DownloadLatestIndex extends EdgarRequest

  case class FileIndex(fileName: String) extends EdgarRequest

  case class ProcessIndexFile(fileContent: String) extends EdgarRequest

  case class DownloadFile(filePath: String)

  case class FilteredFiles(files: List[(String, String, String)])

  case class FileContent(content: String)

  case class FilingInfo(xmlContent: String)

  case object Shutdown

  class IndexRetriever(edgarClient:EdgarModule,
                       indexDir:String) extends Actor {

    val log = Logging(context.system, this)
    def receive = {

      case DownloadLatestIndex => {
        log.info("Retriever. retrieving latest index")
        val latestFile = edgarClient.list(indexDir).last
        sender ! FileIndex(s"$indexDir/$latestFile")
      }

    }

  }

  class IndexProcessor(edgarFileManager: ActorRef) extends Actor {
    val filterFunction = (lineArray:Array[String]) => lineArray.size > 2 && lineArray(2) == "4"
    val log = Logging(context.system, this)
    
    def processContent(content:String):List[(String, String, String)] = {
      val lines = content.split("\n").toList.map(ln => ln.split('|'))
      //lineArray => lineArray.size > 2 && lineArray(2) == "4"
      lines.filter(filterFunction).map(arr=>(arr(0), arr(2), arr(4)))
    }
    
    def receive = {

      case ProcessIndexFile(fileContent: String) => {

        log.info("Processor.Processing")
        val arrList = processContent(fileContent)
        log.info("Sending msg with:" + arrList.size + " elements") 
        edgarFileManager ! FilteredFiles(arrList)

      }

    }

  }

  class EdgarFileSink extends Actor {
    val log = Logging(context.system, this)
    var count = 0

    def receive = {

      case FilingInfo(fileContent: String) => {
        val xmlContent = fileContent.substring(fileContent.indexOf("<ownershipDocument>"), fileContent.indexOf("</XML"))
        
        val xml = XML.loadString(xmlContent)
        
        val issuerName = xml \\ "issuerName"
        val issuerCik = xml \\ "issuerCik"
        val reportingOwnerCik = xml \\ "rptOwnerCik"
        log.info(s"FileSink.$issuerName|$issuerCik|$reportingOwnerCik")
      }

    }

  }

  class EdgarFileManager(downloader: ActorRef, edgarFileSink: ActorRef) extends Actor {

    var fileCount = 0
    val log = Logging(context.system, this)

    def receive = {

      case FilteredFiles(fileList: List[(String, String, String)]) => {

        log.info("FileManager. initialized with:" + fileCount)

        fileList.foreach{case (cik:String, form:String, fileName:String) => downloader ! DownloadFile(fileName)}
      }

      case FileContent(fileContent: String) =>

        edgarFileSink ! FilingInfo(fileContent)

        fileCount -= 1

        if (fileCount == 0) {
          log.info("sending shutdown")
          context stop self
        }

    }

  }

  class Downloader(edgarClient:EdgarModule) extends Actor {

    val log = Logging(context.system, this)
    
    def receive = {

      case DownloadFile(filePath: String) => {

        log.info("downloading:" + filePath)

        val fileContent = edgarClient.downloadFile(filePath)
        
        sender ! FileContent(fileContent)

      }

    }

  }

  class EdgarMaster(retriever: ActorRef, downloader: ActorRef,
                    indexProcessor: ActorRef,
                    edgarFileManager: ActorRef) extends Actor {

    val log = Logging(context.system, this)
    var filesToDownload = 0
    context.watch(edgarFileManager)

    def receive = {

      case Start =>

        log.info("Master.I have now to download the latest index")

        retriever ! DownloadLatestIndex

      case FileIndex(fileName) =>

        log.info("Master.received :" + fileName)

        downloader ! DownloadFile(fileName)

      case FileContent(content) =>

        log.info("Master.+call processor.")

        indexProcessor ! ProcessIndexFile(content)

      case Terminated(edgarFileManager) =>

        log.info("Master shutting down")

        log.info("Shutting down")

        context.system.shutdown()
    }

  }
  
  
  
  

}