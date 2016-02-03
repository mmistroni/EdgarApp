
import akka.actor._
package edgar.actors {

  sealed trait EdgarRequest

  case object Start

  case object DownloadLatestIndex extends EdgarRequest

  case class FileIndex(fileName: String) extends EdgarRequest

  case class ProcessIndexFile(fileContent: String) extends EdgarRequest

  case class DownloadFile(filePath: String)

  case class FilteredFiles(files: List[String])

  case class FileContent(content: String)

  case class FilingInfo(xmlContent: String)

  case object Shutdown

  class IndexRetriever extends Actor {

    def receive = {

      case DownloadLatestIndex => {

        println("Retriever. retrieving latest index")

        sender ! FileIndex("master-idx.txt")

      }

    }

  }

  class IndexProcessor(edgarFileManager: ActorRef) extends Actor {

    def receive = {

      case ProcessIndexFile(fileContent: String) => {

        println("Processor.Processing")

        edgarFileManager ! FilteredFiles(List("/edgar/data/file1", "/edgar/data/file2"))

      }

    }

  }

  class EdgarFileSink extends Actor {

    var count = 0

    def receive = {

      case FilingInfo(fileContent: String) => {

        println("FileSink.Processing content")

        println("Storing...:" + count)

        count += 1

      }

    }

  }

  class EdgarFileManager(downloader: ActorRef, edgarFileSink: ActorRef) extends Actor {

    var fileCount = 0

    def receive = {

      case FilteredFiles(fileList: List[String]) => {

        fileCount = fileList.size

        println("FileManager. initialized with:" + fileCount)

        fileList.foreach(item => downloader ! DownloadFile(item))

        downloader ! FilteredFiles(List("/edgar/data/file1", "/edgar/data/file2"))

      }

      case FileContent(xml: String) =>

        edgarFileSink ! FilingInfo(xml)

        fileCount -= 1

        if (fileCount == 0) {

          println("sending shutdown")

          context.system.shutdown()

        }

    }

  }

  class Downloader extends Actor {

    def receive = {

      case DownloadFile(filePath: String) => {

        println("downloading:" + filePath)

        sender ! FileContent("|CIX23|20911|4|COMPANY DATA|EDGAR/data/files")

      }

    }

  }

  class EdgarMaster(retriever: ActorRef, downloader: ActorRef,

                    indexProcessor: ActorRef,

                    edgarFileManager: ActorRef) extends Actor {

    var filesToDownload = 0

    def receive = {

      case Start =>

        println("Master.I have now to download the latest index")

        retriever ! DownloadLatestIndex

      case FileIndex(fileName) =>

        println("Master.received :" + fileName)

        downloader ! DownloadFile(fileName)

      case FileContent(content) =>

        println("Master.+call processor.")

        indexProcessor ! ProcessIndexFile(content)

      case Shutdown =>

        println("Master shutting down")

        println("Shutting down")

    }

  }

}