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

  val edgarClient = new EdgarModule {
      val ftpClient = new ApacheFTPClient {
        val ftpConfig = new FtpConfig {
          val username = "anonymous"
          val password = "tmp2@gmail.com"
          val host = "ftp.sec.gov"
        }
      }
    }
  
  // Future 1. Retrieve All Filng Files
  def getFilingFiles(filingFiles:List[String], edgarClient:EdgarModule):Future[Seq[String]]  = Future {
      for (filingFile <- filingFiles) yield edgarClient.downloadFile(filingFile)
    }
  // Future 2. Get All Files in MasterDir
  def getEgarMasterIndexesFuture(baseDirName:String, edgarClient:EdgarModule):Future[List[String]]  = Future {
      edgarClient.list(baseDirName)
    }
  
  def extractContent(fileContent:String):List[String] = {
    fileContent.split("\n").toList
  }
  
  // Future 3. process the List of files
  def processList(dirContent:List[String]):Future[List[String]] = Future {
    val latest = dirContent.last
    edgarClient.downloadFile(s"edgar/daily-index/$latest").split("\n").toList
  }
  
  // Future4 . filter lines  we are interested in
  def filterLines(fileLines:List[String]):Future[List[String]] = Future {
    fileLines.filter(line => line.split('|').size > 2 && line.split('|')(2) == "4")
  }
  
  
  val allFilesFuture = getEgarMasterIndexesFuture("edgar/daily-index", edgarClient)
            .flatMap { listOfFiles => processList(listOfFiles) }
            .flatMap { lines => filterLines(lines)}
            
            
  allFilesFuture onSuccess {
    case lines => lines.foreach(println)
  }
              
                  
  /**
  val latestIndexFile = allFilesFuture.map(masterIndexList => masterIndexList.last).map(
                                latestFile => edgarClient.downloadFile(s"edgar/daily-index/$latestFile")).map{ content => extractContent(content)}
                                    
                                    
  latestIndexFile onSuccess {
    case latestIndexLines => latestIndexLines.filter {line => line.split("|").size > 2}.foreach(arr => println(arr))
  }
                                
  
  
  allFilesFuture onSuccess {
    case  latestFileList => {
      val latest = latestFileList.last
      println("lates tis:" + latest)
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
      
 
  **/
  
  Thread.sleep(30000)
  
}