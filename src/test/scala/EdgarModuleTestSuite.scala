import edgar.core._
import org.scalatest._
import org.scalamock.scalatest.MockFactory
import Matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.junit._
import scala.io._
import scala.xml.XML
import org.apache.commons.net.ftp.FTPClient
import scala.io._
import java.io._
import org.apache.commons.net.ftp.FTPFile

import org.mockito.{ Mockito, Matchers=>MockitoMatchers}

@RunWith(classOf[JUnitRunner])
class EdgarModuleTestSuite extends FunSuite with MockFactory with Matchers {

  val testListDirValues = List("listDirs")

  val testFileContent = "fileContent"

  val testDirName = "testDirName"

  val testFileName = "testFileName"

  trait MockFtpClient extends FtpClient {

    def listDirectory(dirName: String): List[String] = testListDirValues

    def retrieveFile(fileName: String): String = testFileContent

  }

  trait MockedFtpClient {

    val ftpClient = Mockito.mock(classOf[FtpClient])
    
    Mockito.when(ftpClient.listDirectory(testDirName)).thenReturn(testListDirValues)
    Mockito.when(ftpClient.retrieveFile(testFileName)).thenReturn(testFileContent)
    
  }

  
   test("FtpClientWithHardCodedMock") {
    object fake extends EdgarModule with MockedFtpClient

    val ftpClient = fake

    ftpClient.list(testDirName) should be(testListDirValues)

    ftpClient.downloadFile(testFileName) should be(testFileContent)
		
  }

  test("testFtpClientWithAbstractClient") {

    val mockFtpClient = Mockito.mock(classOf[FtpClient])

    Mockito.when(mockFtpClient.listDirectory(testDirName)).thenReturn(testListDirValues)
    Mockito.when(mockFtpClient.retrieveFile(testFileName)).thenReturn(testFileContent)
    
    
    
    val edgarClient = new EdgarModule { val ftpClient = mockFtpClient }

    edgarClient.list(testDirName) should be(testListDirValues)
    edgarClient.downloadFile(testFileName) should be (testFileContent)
    
    Mockito.verify(mockFtpClient).listDirectory(testDirName)
    Mockito.verify(mockFtpClient).retrieveFile(testFileName)
    
    
  }
  
  test("testFtpClientWithThinCakePattern") {

    object fake extends EdgarModule with MockedFtpClient

    val edgarClient = fake

    edgarClient.list(testDirName) should be(testListDirValues)

  }
  
  
  class MockApacheFtpClient extends FTPClient {
      
      override def connect(host:String) = {}
      
      override def enterLocalPassiveMode() = {}
    }
  
  
  
  test(" testApacheFTPClient listDirectory") {
  
    val (testUsername, testPassword, testHost, testDir) = ("username", "pwd", "host", "testDir")
    
    val mockFtpClient = Mockito.mock(classOf[MockApacheFtpClient])
    Mockito.when(mockFtpClient.login(testUsername, testPassword)).thenReturn(true)
    Mockito.when(mockFtpClient.listFiles(testDir)).thenReturn(Array[FTPFile]())
    
    val testApacheFtpClient= new  ApacheFTPClient {
      val ftpConfig = new FtpConfig {
        val host = testHost
        val username = testUsername
        val password  = testPassword
      }
      override lazy val ftpClient = mockFtpClient
    } 
    
    testApacheFtpClient.listDirectory(testDir) should be (empty)    
    Mockito.verify(mockFtpClient, Mockito.times(1)).connect(testHost)
    Mockito.verify(mockFtpClient, Mockito.times(1)).login(testUsername, testPassword)
    Mockito.verify(mockFtpClient, Mockito.times(1)).enterLocalPassiveMode
    Mockito.verify(mockFtpClient, Mockito.times(1)).listFiles(testDir)
    Mockito.verify(mockFtpClient, Mockito.times(1)).logout()
    Mockito.verify(mockFtpClient, Mockito.times(1)).disconnect()
    
  }
  
  test(" testApacheFTPClient fileContent") {
  
    val (testUsername, testPassword, testHost, testDir) = ("username", "pwd", "host", "testDir")
    val fileName = "testFileName"
    val fileContent = "testFileContent"
    
    
    
    val mockFtpClient = Mockito.mock(classOf[MockApacheFtpClient])
    val mockInputStream = Mockito.mock(classOf[InputStream])
    Mockito.when(mockFtpClient.login(testUsername, testPassword)).thenReturn(true)
    Mockito.when(mockFtpClient.listFiles(testDir)).thenReturn(Array[FTPFile]())
    Mockito.when(mockFtpClient.retrieveFileStream(fileName)).thenReturn(mockInputStream)
    val testApacheFtpClient = new ApacheFTPClient { 
    
      val ftpConfig = new FtpConfig {
        val host = testHost
        val username = testUsername
        val password  = testPassword
      }
      
      override lazy val ftpClient = mockFtpClient
      
      override def readStream(is:InputStream) = {
        is.available()
        fileContent
      }
    } 
    
    testApacheFtpClient.retrieveFile(fileName)  should be (fileContent)    
    Mockito.verify(mockFtpClient, Mockito.times(1)).connect(testHost)
    Mockito.verify(mockFtpClient, Mockito.times(1)).login(testUsername, testPassword)
    Mockito.verify(mockFtpClient, Mockito.times(1)).enterLocalPassiveMode
    Mockito.verify(mockFtpClient, Mockito.times(1)).logout()
    Mockito.verify(mockFtpClient, Mockito.times(1)).disconnect()
    Mockito.verify(mockInputStream, Mockito.times(1)).available()
    
  }
  
  

}