import scala.io._
import scala.xml.XML
import org.apache.commons.net.ftp.FTPClient
import scala.io._
import java.io._
import org.apache.commons.net.ftp.FTP._
import org.apache.commons.net.ftp.FTPReply
import org.apache.commons.io.IOUtils

package edgar.core {

  /** Below are components of the Cake pattern **/
  trait FtpClientComponent {
    def ftpClient: FTPClient // this is what we mock. But we cannot use it for real
    trait FTPClient {

      def connect(host: String): Unit

      def list(dirName: String): List[String]

      def downloadFile(fileName: String): String
      
      def reconnect :Boolean = true

    }

  }

  trait EdgarDownloader {
    ftpClientComponent: FtpClientComponent =>
    def getIndexFileContent(dirName: String): List[String] = {
      ftpClientComponent.ftpClient.list(dirName)
    }

    def retrieveFilingFile(fileName: String): String = {
      ftpClientComponent.ftpClient.downloadFile(fileName)
    }

  }

  trait FtpClientComponentImpl extends FtpClientComponent {
    def ftpClient = new FTPClientImpl
    class FTPClientImpl extends FTPClient {
      def connect(host: String) = {}
      def list(dirName: String): List[String] = null
      def downloadFile(fileName: String): String = null
    }
  }

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

  trait EdgarModuleCake {
    ftpClient: FtpClient =>
    def list(dirName: String): List[String] = {
      ftpClient.listDirectory(dirName)
    }

    def downloadFile(fileName: String): String = {
      ftpClient.retrieveFile(fileName)
    }

    
    
    
  }

  // Thin Cake Pattern if module is small we only define

  // an abstract method

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
      val reply = ftpClient.getReplyCode();
      if(!FTPReply.isPositiveCompletion(reply)) {
        disconnect();
        println("FTP server refused connection.");
        println(reply)
        throw new IllegalArgumentException("FTP server refused connection." + reply)
      }
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
        if(ftpClient.isConnected()) {
          try {
            ftpClient.disconnect()
          } catch {
            case jle:java.lang.Exception => println("Exception in disconnecting. we do nothing")
          }
          // do nothing
        }
      }
    }

    
    
  }
  
  
  class ApacheFTPClientImpl(_username:String, _password:String, _host:String) extends ApacheFTPClient {
    val ftpConfig = new FtpConfig {
        val username = _username
        val password = _password
        val host = _host
      }
    
    override def readStream(is:InputStream):String = {
      
      IOUtils.toString(is, "UTF-8")
    }
    
  }
  
}
  
  
  

