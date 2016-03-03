import scala.io._
import scala.xml.XML
import org.apache.commons.net.ftp.FTPClient
import scala.io._
import edgar.ftp.FtpFactory
import java.io._
import org.apache.commons.net.ftp.FTP._
import org.apache.commons.net.ftp.FTPReply

import grizzled.slf4j.Logger

package edgar.core {
  
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
      logger.debug(s"FileSink.$issuerName|$issuerCik|$reportingOwnerCik")

    }
    
    def storeXBRLFile(fileList:EdgarTypes.XBRLFiling) = {
      val (first, firstContent) = fileList.head
      logger.info(s"Content for $first is :\n$firstContent")
    }
    
    
  }

  case class EdgarFiling(cik: String, asOfDate: String,
                         formType: String, companyName: String, filingPath: String)

  
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
  
  abstract class DefaultFactory {
    import edgar.ftp.FtpClient
    
    def edgarSink : EdgarSink
    
    def indexProcessor(filterFunction: Array[String] => Boolean):IndexProcessor
    
    def edgarFtpClient(password:String):FtpClient
  }
  
  object EdgarFactory extends DefaultFactory with FtpFactory {
    
    def edgarSink() = new OutputStreamSink{}
    def indexProcessor(filterFunction: Array[String] => Boolean) = new IndexProcessorImpl(filterFunction)
  }
  
  object EdgarTypes {
    type XBRLFiling = List[(String, String)]
    type SimpleFiling = String
  
  }
  
  

}

  
  
  
