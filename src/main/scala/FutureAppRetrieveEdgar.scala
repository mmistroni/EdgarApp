

/**
 * @author marco
 */

import scala.concurrent._

import ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Try, Success, Failure }
import scala.util.{ Success, Failure }
import scala.io._
import scala.xml.XML
import org.apache.commons.net.ftp.FTPClient
import scala.io._
import java.io._

object FutureAppRetrieverEdgar extends App {

  /**  Cake Pattern
   *   1. Define traits for all needed interfaces (FtpClientModule, EdgarDownloaderModule).
   *   2. EdgarDownloaderModule will contains real code apart from FtpClientModule, which is abstract
   *      and will be mixed in at runtime
   *   3. At testing time, what we will do is to create a Test FtpClientModule and inject it into EdgarDownloaderModule,
   *      and at that point we will test our mock data
   *   3.1 once 3 is done we will replace the TestFtpClientModule with a mock and test the mock
   */
  
  
  
  
  val baseFtpUrl = "ftp://ftp.sec.gov/"

  
  abstract class EdgarDownloader {

    def downloadLatestFilingFile(): String

    def downloadFiles(fileList: Seq[String]): List[String]

  }

  class FTPEdgarDownloader(username: String, password: String, host: String) extends EdgarDownloader {

    val ftpClient = new FTPClient()

    private def readStream(is: InputStream) = {
      println("Reading Stream....")
      val reader = new BufferedReader(new InputStreamReader(is))
      try {
        def readLine(reader: BufferedReader, acc: StringBuffer): StringBuffer = {
          val line = reader.readLine()
          if (line == null) acc
          else {
            readLine(reader, acc.append(line).append("\n"))
          }
        }
        readLine(reader, new StringBuffer()).toString
      } finally {
        reader.close()
        is.close()
      }

    }

    private def connect() = {
      ftpClient.connect(host);
      ftpClient.login(username, password);
      ftpClient.enterLocalPassiveMode
    }

    private def downloadFile(fullPath: String): String = {
      println("Fetching:" + fullPath)
      val inputStream = ftpClient.retrieveFileStream(fullPath)
      println("InputStream for :" + fullPath + "is null?" + (inputStream == null))
      readStream(inputStream)
    }

    def downloadLatestFilingFile(): String = {
      connect()
      val baseDir = "edgar/daily-index/"
      val files = ftpClient.listFiles(baseDir).filter(file => file.getName.startsWith("master"))
      val latestFiling = baseDir + files.last.getName
      println(latestFiling)
      try {
        downloadFile(latestFiling)
      } finally {
        disconnect()
      }
    }

    private def executeOperation[T](op: FTPClient => T): T = {
      try {
        connect()
        op(ftpClient)
      } finally {
        disconnect()
      }
    }

    private def downloadFilingFile(filingPath: String): String = {
      connect()
      try {
        println("Downloading filingfile:" + filingPath)
        downloadFile(filingPath)
      } finally {
        disconnect()
      }

    }

    override def downloadFiles(fileList: Seq[String]): List[String] = {

      println("Downloading......" )
      val fileNames = fileList.map(fileName => {
        val path = fileName.substring(0, fileName.indexOf(".")).replace("-", "");
        val file = fileName.split("/").last;
        fileName
        //s"$path/$file"
      })
      val contents = for (fileName <- fileNames) yield downloadFilingFile(fileName)
      contents.toList

    }

    private def disconnect() = {
      ftpClient.logout()
      ftpClient.disconnect()
    }

  }

  def readStream(is: InputStream) = {
    val reader = new BufferedReader(new InputStreamReader(is))
    try {
      def readLine(reader: BufferedReader, acc: StringBuffer): StringBuffer = {
        val line = reader.readLine()
        if (line == null) acc
        else {
          readLine(reader, acc.append(line).append("\n"))
        }
      }
      readLine(reader, new StringBuffer()).toString
    } finally {
      reader.close()
      is.close()
    }

  }

  def retrieveTextFile(fileName: String, f: FTPClient)(op: InputStream => String): String = {
    val inputStream = f.retrieveFileStream(fileName)
    op(inputStream)

  }

  val initializeFTPClientFunc = (ftpClient: FTPClient) => {
    ftpClient.connect("ftp.sec.gov");
    ftpClient.login("anonymous", "mmistroni@gmail.com");
    ftpClient.enterLocalPassiveMode
  }

  val latestFileFunc = (ftpClient: FTPClient) => {
    println("gettin files")
    val files = ftpClient.listFiles().filter(file => file.getName.startsWith("master"))
    val name = files.last
    println(name.getName)
    name.getName
  }

  def testFutureFirstCompleted(): Unit = {

    def getFilingFile(fileNames: Seq[String]): String = {
      val first = fileNames.head
      val path = first.substring(0, first.indexOf(".")).replace("-", "")
      val fileName = first.split("/").last
      val fullPath = s"$path/$fileName"

      println("Fetching:" + fullPath)
      val ftpClient = new FTPClient()
      initializeFTPClientFunc(ftpClient)
      val fileStr = retrieveTextFile(fullPath, ftpClient) { is => readStream(is) }
      ftpClient.disconnect()

      fileStr.substring(fileStr.indexOf("<ownershipDocument>"), fileStr.indexOf("</XML"))

    }

    def getEdgarLines(): Future[Seq[String]] = Future {
      val ftpClient = new FTPClient()
      initializeFTPClientFunc(ftpClient)
      ftpClient.changeWorkingDirectory("edgar/daily-index")
      val latestFile = latestFileFunc(ftpClient)
      val content = retrieveTextFile(latestFile, ftpClient) { is => readStream(is) }
      ftpClient.disconnect()
      content.split("\n")
    }

    def extractLines(lines: Seq[String]): Seq[Array[String]] = {
      lines.map { ln => ln.split('|') } filter { arr => arr.size > 4 && arr(2) == "4" }
    }

    val f = getEdgarLines()
    f onSuccess {
      case lines =>

        val allFilesFuture = Future {
          extractLines(lines) map (arr => arr(4))
        }

        allFilesFuture onSuccess {
          case files =>
            println(files)

            val filingFile = getFilingFile(files)
            println(filingFile)
            val xml = XML.loadString(filingFile)

            val issuer = xml \ "issuer"
            println("______-----------------------")
            println(issuer)
        }

    }

    f onFailure {
      case t => println("An error has occured: " + t.getMessage)
    }

    Thread.sleep(20000);

  }

  def testFtpClient(): Unit = {
    println("Now trying to access FTP site agian...")

    /**
     * val ftpClient = new FTPClient()
     * initializeFTPClientFunc(ftpClient)
     * val latestFile = latestFileFunc(ftpClient)
     * val content = retrieveTextFile(latestFile, ftpClient) { is => readStream(is) }
     * val lines = content.split("\n")
     * lines.foreach(println)
     * ftpClient.disconnect()
     *
     */
    val ftpClient = new FTPEdgarDownloader("anonymous", "mmistroni@gmail.com", "ftp.sec.gov")
    val latestFilingFile = ftpClient.downloadLatestFilingFile()
    val fileContent = latestFilingFile.split("\n")
    val form4Files = fileContent.map(ln => ln.split('|')) filter { arr => arr.size > 4 && arr(2) == "4" } map { arr => arr(4) }
    
    val ftpClient2 = new FTPEdgarDownloader("anonymous", "mmistroni@gmail.com2", "ftp.sec.gov")
    val form4Contents = ftpClient2.downloadFiles(form4Files.take(1)).map {
      fileStr => fileStr.substring(fileStr.indexOf("<ownershipDocument>"), fileStr.indexOf("</XML"))
    }

    for (content <- form4Contents) {
      println("-----------------------------")
      println(content)
    }

  }

  //testFutureFirstCompleted()
  testFtpClient()

}