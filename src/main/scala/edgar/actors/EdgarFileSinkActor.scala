package edgar.actors
import akka.actor._
import akka.actor.SupervisorStrategy.{ Resume, Escalate, Restart }
import akka.event.Logging
import edgar.core.EdgarSink


class EdgarFileSinkActor(sink: EdgarSink) extends Actor with edgar.util.LogHelper {
    val log = Logging(context.system, this)
    var count = 0
    
    def receive = {

      case EdgarRequests.FilingInfo(fileContent: String) => {
        sink.storeFileContent(fileContent)
      }

      case EdgarRequests.FilingXBRLInfo(fileContent: List[(String, String)]) => {
        sink.storeXBRLFile(fileContent)
      }
      
      case Terminated => {
        logger.info("We should send all messages now..")
        sink.emptySink()
        context.system.shutdown()
      }
      

    }

  }
