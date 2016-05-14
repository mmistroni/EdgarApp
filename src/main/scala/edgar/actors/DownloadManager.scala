package edgar.actors

import akka.actor._
import akka.actor.SupervisorStrategy.{ Resume, Escalate, Restart }
import akka.event.Logging
import edgar.ftp.FtpClient
import edgar.core.DefaultFactory
import scala.concurrent.duration._
import java.util.UUID



object DownloadManager {
    import edgar.core.EdgarTypes.{ XBRLFiling, SimpleFiling }
    case class Download(url: String, origin: ActorRef)
    case class XBRLLoadFinished(fileContents: XBRLFiling, origin: ActorRef)
    case class SimpleFilingFinished(fileContent: SimpleFiling, origin: ActorRef)
    case class DownloadFailed(fileContent: String, origin: ActorRef, downloader: ActorRef)
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

          sender ! XBRLLoadFinished(content, origin)
        } else {
          val fileContent = ftpClient.retrieveFile(filePath)
          sender ! SimpleFilingFinished(fileContent, origin)
        }

      }
      case message => log.info(s"unexpected message to to dlownoader:$message")

    }

  }

  class DownloadManager(downloadSlots: Int,
                        factory: DefaultFactory) extends Actor {
    // This class has been copied from 'Learning Concurrent Programming in Scala'
    import scala.collection._
    import DownloadManager._

    val log = Logging(context.system, this)
    val downloaders = mutable.Queue[ActorRef]()
    val pendingWork = mutable.Queue[Download]()
    val workItems = mutable.Map[ActorRef, Download]()

    override val supervisorStrategy =
      OneForOneStrategy(
        maxNrOfRetries = 5, withinTimeRange = 30 seconds) {
          case jns: java.lang.Exception =>
            val originalMessage = workItems.get(sender).get
            log.info(s"Error while Downloading ${originalMessage}: $jns")
            workItems.remove(sender)
            // creating new sender
            log.info(s"Killing Sender..")
            
            sender ! PoisonPill
            downloaders.enqueue(createActor("Replacement"))
            log.info("Enqueuing Sender")
            pendingWork.enqueue(originalMessage)
            Restart // something went wrong. restarting actors
          case _ =>
            Escalate
        }

    private def createActor(actorId: String) = {
      val ftpClient = factory.edgarFtpClient(
        s"$actorId" + UUID.randomUUID().toString() + "@gmail.com")
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
