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

package edgar.core {

  trait LogHelper {
    val loggerName = this.getClass.getName
    lazy val logger = Logger[this.type]
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

  trait IndexProcessor {

    def processIndexFile(fileContent: String): Seq[EdgarFiling]
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

  trait EdgarSink {
    def storeFileContent(fileContent: String)
  }

  trait OutputStreamSink extends EdgarSink with LogHelper {
    def storeFileContent(fileContent: String) = {
      val xmlContent = fileContent.substring(fileContent.indexOf("<ownershipDocument>"), fileContent.indexOf("</XML"))
      val xml = XML.loadString(xmlContent)
      val issuerName = xml \\ "issuerName"
      val issuerCik = xml \\ "issuerCik"
      val reportingOwnerCik = xml \\ "rptOwnerCik"
      logger.info(s"FileSink.$issuerName|$issuerCik|$reportingOwnerCik")

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
    def retrieveStream(fileName: String): InputStream

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

    def retrieveStream(fileName: String): InputStream = {
      execute {
        client =>
          {
            ftpClient.retrieveFileStream(fileName)
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
  
  
  

