import scala.io._
import scala.xml.XML
import org.apache.commons.net.ftp.FTPClient
import scala.io._
import java.io._
import org.apache.commons.net.ftp.FTP._
import org.apache.commons.net.ftp.FTPReply
import org.apache.commons.io.IOUtils
import grizzled.slf4j.Logger
import java.util.zip.ZipInputStream

// TODO: define specific types for XBRL content and for normal content, to avoid having String and List(String, String)
package edgar.core {

  
  object EdgarTypes {
    type XBRLFiling = List[(String, String)]
    type SimpleFiling = String
  
  }
  
  trait LogHelper {
    val loggerName = this.getClass.getName
    lazy val logger = Logger[this.type]
  }
  
  trait IndexProcessor {
    def processIndexFile(fileContent: String): Seq[EdgarFiling]
  }
  
  
  trait EdgarSink {
    def storeFileContent(fileContent: EdgarTypes.SimpleFiling)
    
    def storeXBRLFile(xbrl:EdgarTypes.XBRLFiling)
  }

  trait OutputStreamSink extends EdgarSink with LogHelper {
    def storeFileContent(fileContent: EdgarTypes.SimpleFiling) = {
      val xmlContent = fileContent.substring(fileContent.indexOf("<ownershipDocument>"), fileContent.indexOf("</XML"))
      val xml = XML.loadString(xmlContent)
      val issuerName = xml \\ "issuerName"
      val issuerCik = xml \\ "issuerCik"
      val reportingOwnerCik = xml \\ "rptOwnerCik"
      logger.info(s"FileSink.$issuerName|$issuerCik|$reportingOwnerCik")

    }
    
    def storeXBRLFile(fileList:EdgarTypes.XBRLFiling) = {
      val (first, firstContent) = fileList.head
      logger.info(s"Content for $first is :\n$firstContent")
    }
    
    
  }

  case class EdgarFiling(val cik: String, val asOfDate: String,
                         val formType: String, val companyName: String, val filingPath: String)

  object EdgarPredicates {

    type EdgarFilter = EdgarFiling => Boolean

    val cikEquals: String => EdgarFilter = cik => filing => filing.cik == cik

    val formTypeEquals: String => EdgarFilter = formType => filing => filing.formType == formType

    val companyNameEquals: String => EdgarFilter = companyName => filing => filing.companyName == companyName

    def formTypeIn: Set[String] => EdgarFilter = formTypes => filing => formTypes.contains(filing.formType)

    def cikIn: Set[String] => EdgarFilter = cikList => filing => cikList.contains(filing.cik)

    def and(predicates: Seq[EdgarFilter])(filing: EdgarFiling) = predicates.forall(predicate => predicate(filing))

    def or(predicates: Seq[EdgarFilter])(filing: EdgarFiling) = predicates.exists(predicate => predicate(filing))

  }

  

  class IndexProcessorImpl(filterFunction: Array[String] => Boolean) extends IndexProcessor with LogHelper {

    def processIndexFile(content: String): Seq[EdgarFiling] = {
      val lines = content.split("\n").toList.map(ln => ln.split('|'))
      logger.info("original file has:" + lines.size)
      val res = lines.filter(filterFunction).map(arr => EdgarFiling(arr(0), arr(3),
        arr(2), arr(1),
        arr(4)))
      logger.info(s"After filtering we got:${res.size}")
      res
    }

  }

  

}

package edgar.ftp {

  trait FtpConfig {
    val host: String
    val username: String
    val password: String
  }

  trait FtpClient {
    val ftpConfig: FtpConfig
    def listDirectory(dirName: String): List[String]
    def retrieveFile(fileName: String): String
    def retrieveZippedStream(fileName: String): List[(String, String)]
  }

  object FtpClient {

    def createClient(_username: String, _password: String, _host: String) = {
      new ApacheFTPClient {
        val ftpConfig = new FtpConfig {
          val username = _username
          val password = _password
          val host = _host
        }
      }

    }
  }

  trait EdgarModule {
    val ftpClient: FtpClient

    def list(dirName: String): List[String] = {
      ftpClient.listDirectory(dirName)
    }

    def downloadFile(fileName: String): String = {
      ftpClient.retrieveFile(fileName)
    }
  }

  trait ApacheFTPClient extends FtpClient {

    lazy val ftpClient = new FTPClient()
    val ftpConfig: FtpConfig

    protected def readStream(is: InputStream) = {
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

      ftpClient.connect(ftpConfig.host)
      ftpClient.login(ftpConfig.username, ftpConfig.password)
      ftpClient.enterLocalPassiveMode
      ftpClient.setFileType(BINARY_FILE_TYPE)
      ftpClient.setRemoteVerificationEnabled(false)
      ftpClient.setControlKeepAliveTimeout(300)
    }

    protected def execute[T](op: FTPClient => T): T = {
      try {
        connect()
        op(ftpClient)
      } finally {
        disconnect()
      }
    }

    def listDirectory(dirName: String): List[String] = {
      execute {
        client => client.listFiles(dirName).map(file => file.getName()).filter(fileName => fileName.startsWith("master")).toList
      }
    }

    def retrieveZippedStream(fileName: String): List[(String, String)] = {
      execute {
        client =>
          {

            val xbrlStream = ftpClient.retrieveFileStream(fileName)
            println("Extracting zippe dfile......")
            val zis = new ZipInputStream(xbrlStream)

            def copyStream(istream: InputStream, ostream: OutputStream): Unit = {
              var bytes = new Array[Byte](1024)
              var len = -1
              while ({ len = istream.read(bytes, 0, 1024); len != -1 })
                ostream.write(bytes, 0, len)
            }
            
            
            def extractString(zippedStream: ZipInputStream, accumulator: List[(String, String)]): List[(String, String)] = {
              val entry = zippedStream.getNextEntry()
              if (entry == null) {
                accumulator
              } else {
                val currentFile = entry.getName()
                val outstream = new ByteArrayOutputStream(1024)
                copyStream(zippedStream, outstream)
                extractString(zippedStream, (currentFile, outstream.toString) :: accumulator)
              }

            }

            val res = extractString(zis, List[(String, String)]())
            zis.close()
            res
            
          }
      }
    }

    def retrieveFile(fileName: String): String = {
      execute {
        client =>
          {
            val inputStream = ftpClient.retrieveFileStream(fileName)
            readStream(inputStream)
          }
      }
    }

    private def disconnect() = {
      try {
        ftpClient.logout()
      } finally {
        try {
          ftpClient.disconnect()

        } catch {
          case jle: java.lang.Exception => print("Exception in disconnecting. we do nothing")
        }
        // do nothing

      }
    }

  }

  class ApacheFTPClientImpl(_username: String, _password: String, _host: String) extends ApacheFTPClient with edgar.core.LogHelper {
    val ftpConfig = new FtpConfig {
      val username = _username
      val password = _password
      val host = _host
    }

    override def readStream(is: InputStream): String = {
      println(is.getClass().getName());

      IOUtils.toString(is, "UTF-8")
    }

  }

}
  
  
  

