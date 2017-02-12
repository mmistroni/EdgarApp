package edgar.ftp

import org.apache.commons.net.ftp.FTPClient
import scala.io._
import java.io._
import org.apache.commons.net.ftp.FTP._
import org.apache.commons.net.ftp.FTPReply
import java.util.zip.ZipInputStream
import org.apache.commons.io.IOUtils


trait ApacheFTPClient extends FtpClient with edgar.util.LogHelper {

  lazy val ftpClient = new FTPClient() // Commons.Net FTP client
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
    logger.info("connecting to :" + ftpConfig.host)
    ftpClient.connect(ftpConfig.host)
    ftpClient.login(ftpConfig.username, ftpConfig.password)
    ftpClient.enterLocalPassiveMode
    
  }

  def connected: Boolean = ftpClient.isConnected

  protected def execute[T](op: FTPClient => T): T = {
    try {
      if (!ftpClient.isConnected) {
        connect()
      }
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

  def disconnect() = {
    //try {
    //  ftpClient.logout()
    //} finally {
      try {
        ftpClient.disconnect()

      } catch {
        case jle: java.lang.Exception => print("Exception in disconnecting. we do nothing")
      }
      // do nothing

    //}
  }

}
