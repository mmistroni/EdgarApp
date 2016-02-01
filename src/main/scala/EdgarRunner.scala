import scala.concurrent._

import ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Try, Success, Failure }
import scala.util.{ Success, Failure }
import scala.io._
import scala.xml.XML

import edgarmodule._
object EdgarRunner extends App {
  println("Kicking off EdgarModule")

  def getFilingFiles(filingFiles:List[String], edgarClient:EdgarModule):Future[Seq[String]]  = {
    Future {
      for (filingFile <- filingFiles) yield edgarClient.downloadFile(filingFile)
    }
  }
  
  
  def getEgarMasterIndexesFuture(baseDirName:String, edgarClient:EdgarModule):Future[List[String]]  = {
    Future {
      edgarClient.list(baseDirName)
    }
  }
  
  def getEdgarClient(hostPwd:String):EdgarModule = {
    new EdgarModule {
      val ftpClient = new ApacheFTPClient {
        val ftpConfig = new FtpConfig {
          val username = "anonymous"
          val password = hostPwd
          val host = "ftp.sec.gov"
        }
      }
    }
  }

  def extractContent(fileContent:String):List[String] = {
    fileContent.split("\n").toList
  }
  
  
  val edgarClient = getEdgarClient("tmp")
  
  val allFilesFuture = getEgarMasterIndexesFuture("edgar/daily-index", edgarClient)
  
  allFilesFuture onSuccess {
    case  latestFileList => {
      val latest = latestFileList.last
      val fut = Future {
              edgarClient.downloadFile(s"edgar/daily-index/$latest")
      }
      
      fut onSuccess {
        case content => {
            val contentFut = Future {  
                                        val fileContent = extractContent(content).filter {line => line.split("|").size > 2}
                                        fileContent.map { ln => ln.split('|') } filter { arr => arr.size > 4 && arr(2) == "4" }
                                    } 
        
            contentFut onSuccess {
              case lines => {
                          lines.foreach(arr => println((arr(0), arr(2), arr(4))))
                
              }
            }
            
        }
      }
    }
  }
  
  allFilesFuture onFailure {
    case ex => println("Exception on all files future :"  +ex)
  }
      
 
 
  
  Thread.sleep(20000)
  
}