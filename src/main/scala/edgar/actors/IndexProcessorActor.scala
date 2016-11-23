package edgar.actors

import akka.actor._
import akka.actor.SupervisorStrategy.{ Resume, Escalate, Restart }
import akka.event.Logging
import akka.actor._
import akka.actor.SupervisorStrategy.{ Resume, Escalate, Restart }
import akka.event.Logging
import edgar.core.IndexProcessor

class IndexProcessorActor(indexProcessor: IndexProcessor, edgarFileManager: ActorRef) 
      extends Actor {

  
    val log = Logging(context.system, this)
  
    def receive = {

      case EdgarRequests.ProcessIndexFile(fileContent: String) => {
        val arrList = indexProcessor.processIndexFile(fileContent)
        log.debug("Sending msg with:" + arrList.size + " elements")
        if (arrList.isEmpty) {
          log.info("No filing found. shutting down....")
          context.system.shutdown()
        } else {
          edgarFileManager ! EdgarRequests.FilteredFiles(arrList.toList)
        }
      }

    }

  }
