import edgar.ftp._
import org.scalatest._
import Matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import edgar.core._
import org.junit._
import scala.io._
import scala.xml.XML
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE
import scala.io._
import java.io._
import org.apache.commons.net.ftp.FTPFile
import Assert._
import edgar.predicates.EdgarPredicates._

import org.mockito.{ Mockito, Matchers=>MockitoMatchers}
import akka.testkit._


@RunWith(classOf[JUnitRunner])
class HttpsFtpClientTetSuite extends FunSuite  with Matchers {

  val testListDirValues = List("listDirs")
  val testFileContent = "fileContent"
  val testDirName = "testDirName"
  val baseDir = "baseDir"
  val testFileName = "testFileName"
  val mockedWebCrawler = Mockito.mock(classOf[edgar.util.WebCrawler]) 
  
  val mockHttpsFtpClient = new HttpsFtpClient(baseDir)  {
    override val crawler = mockedWebCrawler
  }
  
  test("ListDirectory for HttpsFtpClient") {
    val expectedCrawledDir = s"${mockHttpsFtpClient.edgarDir}/testDirName"
    Mockito.when(mockedWebCrawler.crawlDirectory(expectedCrawledDir)).thenReturn(testListDirValues)
    val results = mockHttpsFtpClient.listDirectory(testDirName)
    results should be (testListDirValues)
    Mockito.verify(mockedWebCrawler).crawlDirectory(expectedCrawledDir)
    
  }  

  /**
  test("RetrieveFileName for HttpsFtpClient") {
    val expectedCrawledDir = s"$baseDir/testDirName"
    Mockito.when(mockedWebCrawler.crawlDirectory(expectedCrawledDir)).thenReturn(testListDirValues)
    val results = mockHttpsFtpClient.listDirectory(testDirName)
    results should be (testListDirValues)
    Mockito.verify(mockedWebCrawler).crawlDirectory(expectedCrawledDir)
    
  }
  
  **/
  
}