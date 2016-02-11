
import akka.actor._
import akka.actor.SupervisorStrategy.{ Resume, Escalate, Restart }
import edgar.ftp._
import scala.xml.XML
import akka.event.Logging
import scala.concurrent.duration._
import java.util.UUID

package edgar.actors {

  object DownloadManager {
    case class Download(url: String, origin: ActorRef)
    case class Finished(fileContent: String, origin: ActorRef)
    case class DownloadFailed(fileContent: String, origin: ActorRef, downloader: ActorRef)
  }

  object EdgarRequests {
  
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

  }
  
  class IndexRetriever(indexProcessor:ActorRef, 
                       downloader:ActorRef,
                       ftpClient:FtpClient,
                       indexDir: String) extends Actor {

    val log = Logging(context.system, this)
    
    def receive = {

      case EdgarRequests.DownloadLatestIndex => {
        log.info("Retriever. retrieving latest index")
        val latestFile = ftpClient.listDirectory(indexDir).last
        log.info(s"Sending data to downloader to retireve:$latestFile")
        downloader ! EdgarRequests.DownloadFile(s"$indexDir/$latestFile")
      }

      case EdgarRequests.FileContent(content) => {
        log.info("Master.+call processor.")
        indexProcessor ! EdgarRequests.ProcessIndexFile(content)
      }

    }

  }

  class IndexProcessor(edgarFileManager: ActorRef) extends Actor {
    val filterFunction = (lineArray: Array[String]) => lineArray.size > 2 && lineArray(2) == "4"
    val log = Logging(context.system, this)

    def processContent(content: String): List[(String, String, String)] = {
      val lines = content.split("\n").toList.map(ln => ln.split('|'))
      log.info("original file has:" + lines.size)
      lines.filter(filterFunction).map(arr => (arr(0), arr(2), arr(4)))
    }

    def receive = {

      case EdgarRequests.ProcessIndexFile(fileContent: String) => {

        log.info("Processor.Processing")
        val arrList = processContent(fileContent)
        log.info("Sending msg with:" + arrList.size + " elements")
        edgarFileManager ! EdgarRequests.FilteredFiles(arrList)

      }

    }

  }

  class EdgarFileSink extends Actor {
    val log = Logging(context.system, this)
    var count = 0

    def receive = {

      case EdgarRequests.FilingInfo(fileContent: String) => {
        val xmlContent = fileContent.substring(fileContent.indexOf("<ownershipDocument>"), fileContent.indexOf("</XML"))

        val xml = XML.loadString(xmlContent)

        val issuerName = xml \\ "issuerName"
        val issuerCik = xml \\ "issuerCik"
        val reportingOwnerCik = xml \\ "rptOwnerCik"
        log.debug(s"FileSink.$issuerName|$issuerCik|$reportingOwnerCik")
      }

    }

  }

  class EdgarFileManager(downloader: ActorRef, edgarFileSink: ActorRef) extends Actor {

    var fileCount = 0
    val log = Logging(context.system, this)

    def receive = {

      case EdgarRequests.FilteredFiles(fileList: List[(String, String, String)]) => {
        fileCount = fileList.size
        fileList.foreach { case (cik: String, form: String, fileName: String) => downloader ! EdgarRequests.DownloadFile(fileName) }

      }

      case EdgarRequests.FileContent(fileContent: String) =>
        edgarFileSink ! EdgarRequests.FilingInfo(fileContent)

        fileCount -= 1
        log.debug(s"$fileCount remaining to download.....")
        if (fileCount == 0) {
          log.info("sending shutdown")
          downloader ! PoisonPill

          //context stop self
        }

    }

  }

  class EdgarMaster(retriever: ActorRef, downloader: ActorRef,
                    indexProcessor: ActorRef,
                    edgarFileManager: ActorRef) extends Actor {
    import DownloadManager.Download

    val log = Logging(context.system, this)
    var filesToDownload = 0
    var startTime: Long = _
    var endTime: Long = _
    context.watch(downloader)

    def receive = {

      case EdgarRequests.Start =>
        log.info("Master.I have now to download the latest index")
        startTime = System.currentTimeMillis()
        retriever ! EdgarRequests.DownloadLatestIndex

      
      case Terminated(downoader) =>
        endTime = System.currentTimeMillis()
        log.info("Master shutting down")
        val finalTime = (endTime - startTime) / 1000
        log.info(s"Shutting down. All downloaded in $finalTime seconds ")

        context.system.shutdown()

      case message => log.info(s"Unexpected msg:$message")
    }

  }

  class ChildDownloader(ftpClient: FtpClient) extends Actor {

    import DownloadManager._
    val log = Logging(context.system, this)
    
    override def preStart(): Unit = {
      log.info("Starting downloader again")
    }

    override def preRestart(reason: Throwable, msg: Option[Any]): Unit = {
      log.info("Restarting downloader again with new ftpclient")
      
    }

    def receive = {

      case Download(filePath: String, origin) => {
        val fileContent = ftpClient.retrieveFile(filePath)
        sender ! Finished(fileContent, origin)

      }
      case message => log.info(s"XXXXunexpected message to to dlownoader:$message")

    }

  }

  class DownloadManager(val downloadSlots: Int) extends Actor {
    // This class has been copied from 'Learning Concurrent Programming in Scala'
    import scala.collection._
    import DownloadManager._

    val log = Logging(context.system, this)
    val downloaders = mutable.Queue[ActorRef]()
    val pendingWork = mutable.Queue[Download]()
    val workItems = mutable.Map[ActorRef, Download]()

    override val supervisorStrategy =
      OneForOneStrategy(
        maxNrOfRetries = 20, withinTimeRange = 2 seconds) {
          case jns: java.lang.Exception =>
            val originalMessage = workItems.get(sender).get
            log.info(s"Error while Downloading ${originalMessage}: $jns")
            log.info(s"$sender")
            workItems.remove(sender)
            // creating new sender
            log.info(s"before restart we have ${downloaders.size}")
            log.info("Sender Removed")
            downloaders.enqueue(sender)
            log.info("Enqueuing Sender")
            log.info(s"after enqueuing  we have ${downloaders.size}")
            
            pendingWork.enqueue(originalMessage)
            Restart // something went wrong. restarting actors
          case _ =>
            Escalate
        }

    private def createActor(actorId:String) = {
      val ftpClient = new ApacheFTPClient {
          val ftpConfig = new FtpConfig {
            val username = "anonymous"
            val password = s"$actorId" + UUID.randomUUID().toString() + "@gmail.com"
            val host = "ftp.sec.gov"
          }
        }
        context.actorOf(Props(classOf[ChildDownloader], ftpClient), actorId)
    }
    
    override def preStart(): Unit = {
      for (i <- 0 until downloadSlots) {
        
        val dl = createActor(s"dl$i")
        downloaders.enqueue(dl)
      }
      log.info("Initialization finished.downloder queue size:" + downloaders.size)
    }

    private def checkDownloads(): Unit = {
      log.debug(s"Checking downloads.workQueue:${pendingWork.size}|worker queue:${downloaders.size}....")
      if (pendingWork.nonEmpty && downloaders.nonEmpty) {
        try {
          val dl = downloaders.dequeue()
          val item = pendingWork.dequeue()
          log.info(
            s"$item starts, -- ${downloaders.size} ${downloaders} download slots left")
          dl ! item
          workItems(dl) = item
        } catch {
          case ioe: java.lang.Exception => log.info("Exception in checkDownoad. should remove msg")
        }
      } 
    }
    def receive = {
      case EdgarRequests.DownloadFile(filePath) =>
        try {
          pendingWork.enqueue(Download(filePath, sender))
          //log.info("Pending Queue size:" + pendingWork.size)
          checkDownloads()
        } catch {
          case ioe: java.lang.Exception => log.info("Downloadmgr.exception:" + ioe.getMessage())
        }

      case DownloadFailed(path, origin, actorRef) =>
        log.info(s"Failed Download for $path from $actorRef")
        workItems.remove(actorRef)
        pendingWork.enqueue(Download(path, origin))
        checkDownloads()

      case Finished(content, origin) =>
        origin ! EdgarRequests.FileContent(content)
        workItems.remove(sender)
        downloaders.enqueue(sender)
        log.debug(
          s" done, ${downloaders.size} download slots left")
        checkDownloads()
      case message =>
        log.info(s"received unexpected message:")
    }
  }

}