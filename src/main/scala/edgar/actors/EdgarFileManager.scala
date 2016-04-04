package edgar.actors
import akka.actor._
import akka.actor.SupervisorStrategy.{ Resume, Escalate, Restart }
import akka.event.Logging
import edgar.core.EdgarFiling


class EdgarFileManager(downloader: ActorRef, edgarFileSink: ActorRef) extends Actor {
    import scala.collection.mutable.{ Map => MutableMap }
    import edgar.actors.EdgarRequests._
    val log = Logging(context.system, this)
    var count = 0
    context.watch(edgarFileSink)

    private def createMessage(filing: EdgarFiling) = {
      log.info(filing.toString)
      EdgarRequests.createEdgarFilingMessage(filing)
    }

    def receive = {

      case EdgarRequests.FilteredFiles(fileList: Seq[EdgarFiling]) => {
        count = fileList.size
        fileList.foreach { edgarFiling: EdgarFiling => downloader ! createMessage(edgarFiling) }
      }

      case EdgarRequests.FileContent(fileContent: String) =>
        edgarFileSink ! EdgarRequests.FilingInfo(fileContent)

        count -= 1
        log.info(s"$count remaining to download.....")
        if (count == 0) {
          log.info("sending shutdown")
          downloader ! PoisonPill
          edgarFileSink ! Terminated

        }

      case EdgarRequests.StreamContent(fileContent: List[(String, String)]) =>
        log.info("Retrieved Edgar XBRL file:" + fileContent.size)
        edgarFileSink ! FilingXBRLInfo(fileContent)

        count -= 1
        //fileMap.remove(fileContent)
        log.debug(s"$count remaining to download.....")
        if (count == 0) {
          log.info("sending shutdown")
          downloader ! PoisonPill
          edgarFileSink ! Terminated
        }
        
      
    }

  }
