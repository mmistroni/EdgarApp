import scala.io._
import scala.xml.XML
import org.apache.commons.net.ftp.FTPClient
import scala.io._
import java.io._
import org.apache.commons.net.ftp.FTP._
import org.apache.commons.net.ftp.FTPReply
import org.apache.commons.io.IOUtils

package edgar.core {

  case class EdgarFiling(val cik: String, val asOfDate: String,
                         val formType: String, val companyName: String, val filingPath: String)

  object EdgarPredicates {

    def cikEquals(value: String)(filing: EdgarFiling) = filing.cik == value

    def formTypeEquals(value: String)(filing: EdgarFiling) = filing.formType == value

    def companyNameEquals(value: String)(filing: EdgarFiling) = filing.companyName == value

    def formTypeIn(formTypes: List[String])(filing: EdgarFiling) = formTypes.contains(filing.formType)

    def cikIn(ciks: List[String])(filing: EdgarFiling) = ciks.contains(filing.cik)

    def and(predicates: Seq[EdgarFiling => Boolean])(filing: EdgarFiling) = predicates.forall(predicate => predicate(filing))

    def or(predicates: Seq[EdgarFiling => Boolean])(filing: EdgarFiling) = predicates.exists(predicate => predicate(filing))

  }

  trait IndexProcessor {

    def processIndexFile(fileContent: String): Seq[(String, String, String)]
  }

  class IndexProcessorImpl(filterFunction: Array[String] => Boolean) extends IndexProcessor {

    def processIndexFile(content: String): Seq[(String, String, String)] = {
      val lines = content.split("\n").toList.map(ln => ln.split('|'))
      println("original file has:" + lines.size)
      val res = lines.filter(filterFunction).map(arr => (arr(0), arr(2), arr(4)))
      println(s"After filtering we got:${res.size}")
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

    private def execute[T](op: FTPClient => T): T = {
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
          case jle: java.lang.Exception => println("Exception in disconnecting. we do nothing")
        }
        // do nothing

      }
    }

  }

  class ApacheFTPClientImpl(_username: String, _password: String, _host: String) extends ApacheFTPClient {
    val ftpConfig = new FtpConfig {
      val username = _username
      val password = _password
      val host = _host
    }

    override def readStream(is: InputStream): String = {

      IOUtils.toString(is, "UTF-8")
    }

  }

}
  
  
  

