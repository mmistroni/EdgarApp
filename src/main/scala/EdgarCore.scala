import scala.io._
import scala.xml.XML
import org.apache.commons.net.ftp.FTPClient
import scala.io._
import edgar.ftp.FtpFactory
import edgar.predicates.EdgarPredicates.EdgarFilter

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
    /**
   * Simple text filings
   *
   * <documentType>4</documentType>
   *
   * <periodOfReport>2016-02-25</periodOfReport>
   *
   * <issuer>
   * <issuerCik>0001000623</issuerCik>
   *
   *
   *
   * XBRL filings
   * -<xbrli:context id="FD2015Q4YTD">
   *
   *
   * -<xbrli:entity>
   *
   * <xbrli:identifier scheme="http://www.sec.gov/CIK">0001000623</xbrli:identifier>
   *
   * </xbrli:entity>
   *
   *
   * -<xbrli:period>
   *
   * <xbrli:startDate>2015-01-01</xbrli:startDate>
   *
   * <xbrli:endDate>2015-12-31</xbrli:endDate>
   *
   * </xbrli:period>
   *
   * </xbrli:context>
   *
   *
   *
   */

    
    
    def storeFileContent(fileContent: EdgarTypes.SimpleFiling)
    
    def storeXBRLFile(xbrl:EdgarTypes.XBRLFiling)
  }

  trait OutputStreamSink extends EdgarSink with LogHelper {
    def storeFileContent(fileContent: EdgarTypes.SimpleFiling) = {
      if (fileContent.indexOf("<ownershipDocument>") >= 0) {
        val xmlContent = fileContent.substring(fileContent.indexOf("<ownershipDocument>"), fileContent.indexOf("</XML"))
        val xml = XML.loadString(xmlContent)
        val issuerName = xml \\ "issuerName"
        val issuerCik = xml \\ "issuerCik"
        val reportingOwnerCik = xml \\ "rptOwnerCik"
        logger.info(s"FileSink.$issuerName|$issuerCik|$reportingOwnerCik")
      } else {
        logger.info("Invalid XML content..")
      }
    }
    
    def storeXBRLFile(fileList:EdgarTypes.XBRLFiling) = {
      val (first, firstContent) = fileList.head
      logger.info(s"Content for $first is :\n$firstContent")
    }
    
    
  }

  case class EdgarFiling(cik: String, asOfDate: String,
                         formType: String, companyName: String, filingPath: String)

  
  class IndexProcessorImpl(filterFunction:EdgarFilter) extends IndexProcessor with LogHelper {
    
    def processIndexFile(content: String): Seq[EdgarFiling] = {
      val lines = content.split("\n").toList.map(ln => ln.split('|')).filter(arr=>arr.size > 2)
                      .map(arr => EdgarFiling(arr(0), arr(3),
                                              arr(2), arr(1),
                                              arr(4)))
      logger.info("original file has:" + lines.size)
      logger.info(lines)
      val res = lines.filter(filterFunction)
      logger.info(s"After filtering we got:${res.size}")
      res
    }

  }
  
  abstract class DefaultFactory {
    import edgar.ftp.FtpClient
    
    def edgarSink : EdgarSink
    
    def indexProcessor(filterFunction: EdgarFilter):IndexProcessor
    
    def edgarFtpClient(password:String):FtpClient
  }
  
  object EdgarFactory extends DefaultFactory with FtpFactory {
    
    def edgarSink() = new OutputStreamSink{}
    def indexProcessor(filterFunction: EdgarFilter) = new IndexProcessorImpl(filterFunction)
  }
  
  object EdgarTypes {
    type XBRLFiling = List[(String, String)]
    type SimpleFiling = String
  
  }
  
  

}

  
  
  

