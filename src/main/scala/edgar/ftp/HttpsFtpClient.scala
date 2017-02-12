package edgar.ftp
import scala.io._
import edgar.util.WebCrawler
import java.util.zip.ZipInputStream
import org.apache.commons.io.IOUtils
import java.io._

class HttpsFtpClient(baseDir: String)
    extends FtpClient
    with edgar.util.LogHelper {
  // This module reads file using Https. 
  // This replaces the FTP client as Edgar no longer serves files via FTP
  // more work as we need to replace reading from stream to read zipped files
  // 
  val crawler = new WebCrawler {}
  val ftpConfig = null
  val edgarDir = "https://www.sec.gov/Archives/"
  
  def listDirectory(dirName: String): Seq[String] = {
    val fullDir = s"$edgarDir/$dirName"
    logger.info(s"Crawling directory:$fullDir")
    crawler.crawlDirectory(fullDir)

  }

  def retrieveFile(fileName: String): String = {
    logger.info(s"baseDir is:$edgarDir")
    logger.info(s"filename is:$fileName")
    val fullPath = fileName.indexOf("http") match {
      case 0 => fileName
      case _  => s"$edgarDir/$fileName"
    }
    logger.info(s"Retrieving file:$fullPath")
    readFileContent(fullPath)
  }

  def retrieveZippedStream(fileName: String): List[(String, String)] = {
    val xbrlStream = getInputStreamFromURL(fileName)
    println("Extracting zippe dfile......")
    val zis = new ZipInputStream(xbrlStream)

    val res = extractString(zis, List[(String, String)]())
    zis.close()
    res
  }

  def disconnect: Unit = {}

  private def readFileContent(fileName:String) = Source.fromURL(fileName).mkString
  
  
  private def getInputStreamFromURL(urlString: String): InputStream = {
    new java.net.URL(urlString).openStream();
  }

  private def copyStream(istream: InputStream, ostream: OutputStream): Unit = {
    var bytes = new Array[Byte](1024)
    var len = -1
    while ({ len = istream.read(bytes, 0, 1024); len != -1 })
      ostream.write(bytes, 0, len)
  }

  private def extractString(zippedStream: ZipInputStream, accumulator: List[(String, String)]): List[(String, String)] = {
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

}