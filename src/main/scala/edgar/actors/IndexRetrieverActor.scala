package edgar.actors
import akka.actor._
import akka.actor.SupervisorStrategy.{ Resume, Escalate, Restart }
import akka.event.Logging

import edgar.ftp.FtpClient


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