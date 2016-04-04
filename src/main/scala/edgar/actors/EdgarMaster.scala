package edgar.actors

import akka.actor._
import akka.actor.SupervisorStrategy.{ Resume, Escalate, Restart }
import akka.event.Logging


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

      
      case message => log.info(s"Unexpected msg:$message")
    }

  }
