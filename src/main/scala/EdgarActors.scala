
import akka.actor._
import akka.actor.SupervisorStrategy.{ Resume, Escalate, Restart }
import edgar.ftp._
import edgar.core._
import scala.xml.XML
import akka.event.Logging
import scala.concurrent.duration._
import java.util.UUID
import java.io.InputStream
import org.apache.commons.io.IOUtils


  
package edgar.actors {


  
  
  object DownloadManager {
    import EdgarTypes.{XBRLFiling, SimpleFiling}
    case class Download(url: String, origin: ActorRef)
    case class XBRLLoadFinished(fileContents: XBRLFiling, origin: ActorRef)
    case class SimpleFilingFinished(fileContent: SimpleFiling, origin: ActorRef)
    case class DownloadFailed(fileContent: String, origin: ActorRef, downloader: ActorRef)
  }

  object EdgarRequests {

    import EdgarTypes.{SimpleFiling, XBRLFiling}
  
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

    case object Shutdown

    def createEdgarFilingMessage(filing:EdgarFiling):EdgarRequest = filing.formType match {
      case xbrl if List("10-K", "20-F", "40-F", "10-Q", "8-K", "6-K").contains(xbrl) => {
        val filePath = filing.filingPath;
        val adjustedPath = filePath.substring(0, filePath.indexOf(".")).replace("-", "")
        val fileName = filePath.split("/").last.replace(".txt", "-xbrl.zip")
        val fullPath = s"$adjustedPath/$fileName"
        DownloadFile(fullPath)
      }
      case _ => DownloadFile(filing.filingPath)
    }
    
    
  }

  class IndexRetrieverActor(indexProcessor: ActorRef,
                            downloader: ActorRef,
                            ftpClient: FtpClient,
                            indexDir: String) extends Actor {

    val log = Logging(context.system, this)

    def receive = {
      case EdgarRequests.DownloadLatestIndex => {
        val latestFile = ftpClient.listDirectory(indexDir).last
        log.debug(s"Sending data to downloader to retireve:$latestFile")
        downloader ! EdgarRequests.DownloadFile(s"$indexDir/$latestFile")
      }
      case EdgarRequests.FileContent(content) => {
        log.info("Master.+call processor.")
        indexProcessor ! EdgarRequests.ProcessIndexFile(content)
      }
      case message => log.info("Unhandled" + message);

    }

  }

  class IndexProcessorActor(indexProcessor: IndexProcessor, edgarFileManager: ActorRef) extends Actor {

    val log = Logging(context.system, this)

    def receive = {

      case EdgarRequests.ProcessIndexFile(fileContent: String) => {
        val arrList = indexProcessor.processIndexFile(fileContent) //processContent(fileContent)
        log.debug("Sending msg with:" + arrList.size + " elements")
        edgarFileManager ! EdgarRequests.FilteredFiles(arrList.toList)

      }

    }

  }

  class EdgarFileSinkActor(sink:EdgarSink) extends Actor {
    val log = Logging(context.system, this)
    var count = 0

    def receive = {

      case EdgarRequests.FilingInfo(fileContent: String) => {
        sink.storeFileContent(fileContent)
      }
      
      case EdgarRequests.FilingXBRLInfo(fileContent: List[(String, String)]) => {
        sink.storeXBRLFile(fileContent)
      }

    }

  }

  class EdgarFileManager(downloader: ActorRef, edgarFileSink: ActorRef) extends Actor {
    import scala.collection.mutable.{ Map => MutableMap }
    import edgar.actors.EdgarRequests._
    val log = Logging(context.system, this)
    var count = 0 

    private def createMessage(filing: EdgarFiling) = EdgarRequests.createEdgarFilingMessage(filing)

    def receive = {

      case EdgarRequests.FilteredFiles(fileList: Seq[EdgarFiling]) => {
        count = fileList.size
        fileList.foreach { edgarFiling: EdgarFiling => downloader ! createMessage(edgarFiling) }
      }

      case EdgarRequests.FileContent(fileContent: String) =>
        edgarFileSink ! EdgarRequests.FilingInfo(fileContent)

        count -= 1
        log.debug(s"$count remaining to download.....")
        if (count == 0) {
          log.info("sending shutdown")
          downloader ! PoisonPill

      }

      case EdgarRequests.StreamContent(fileContent:List[(String, String)]) =>
        log.info("Retrieved Edgar XBRL file:" + fileContent.size)
        edgarFileSink ! FilingXBRLInfo(fileContent)
        
        count -= 1
        //fileMap.remove(fileContent)
        log.debug(s"$count remaining to download.....")
        if (count == 0) {
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
        if (filePath.endsWith(".zip")) {
          log.info("Extracting XBRL ifle....")
          val content = ftpClient.retrieveZippedStream(filePath)
          
          sender !XBRLLoadFinished(content, origin)
        } else {
            val fileContent = ftpClient.retrieveFile(filePath)
            sender ! SimpleFilingFinished(fileContent, origin)
        }
        
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

    private def createActor(actorId: String) = {
      val ftpClient = FtpClient.createClient(
                      "anonymous", 
                      s"$actorId" + UUID.randomUUID().toString() + "@gmail.com", 
                      "ftp.sec.gov")
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
          checkDownloads()
        } catch {
          case ioe: java.lang.Exception => log.info("Downloadmgr.exception:" + ioe.getMessage())
        }

      case DownloadFailed(path, origin, actorRef) =>
        log.info(s"Failed Download for $path from $actorRef")
        workItems.remove(actorRef)
        pendingWork.enqueue(Download(path, origin))
        checkDownloads()

      case SimpleFilingFinished(content, origin) =>
        origin ! EdgarRequests.FileContent(content)
        workItems.remove(sender)
        downloaders.enqueue(sender)
        log.debug(
          s" done, ${downloaders.size} download slots left")
        checkDownloads()
        
      case XBRLLoadFinished(content, origin) =>
        log.info("Stream Finished...")
        origin ! EdgarRequests.StreamContent(content)
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