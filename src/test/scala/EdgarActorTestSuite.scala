
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.junit._

import Assert._
import edgar.actors._
import edgar.actors.EdgarRequests._
import edgar.actors.DownloadManager._
import edgar.ftp._
import edgar.core._
import edgar.actors.DownloadManager._
import akka.actor.ActorSystem
import akka.actor.{ ActorRef, Props, Terminated }
import akka.actor.Actor
import akka.actor.Props
import akka.testkit.{ TestKit, TestActorRef, ImplicitSender, TestProbe }
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import org.mockito._
import org.mockito.Mockito._

class EdgarActorTestSuite extends TestKit(ActorSystem("testSystem")) with ImplicitSender {
  
  def createMockFtpClient(testFileName:String, testFileContent:String) = {
    val mockFtpClient = Mockito.mock(classOf[FtpClient])
    when(mockFtpClient.retrieveFile(testFileName)).thenReturn(testFileContent)
    mockFtpClient
  }
  
    
  @Test def testRetriever() {
    val indexFile = "IndexFile"
    val baseDir = "baseDir"
    val testFilePath = s"$baseDir/$indexFile"
    val testFileContent = "|CIX23|20911|4|COMPANY DATA|EDGAR/data/files"
    val mockFtpClient = Mockito.mock(classOf[FtpClient])
    when(mockFtpClient.listDirectory(baseDir)).thenReturn(List(indexFile))
    val downloaderProbe = TestProbe() 
    val indexProcessorProbe = TestProbe()
    val retriever = TestActorRef(Props(classOf[IndexRetrieverActor], indexProcessorProbe.ref,
                                                                     downloaderProbe.ref,
                                                                     mockFtpClient,
                                                                     baseDir))

    within(1000 millis) {
      retriever ! DownloadLatestIndex
      downloaderProbe.expectMsg(1000 millis, DownloadFile(testFilePath));
      downloaderProbe.reply(FileContent(testFileContent))
      indexProcessorProbe.expectMsg(1000 millis ,ProcessIndexFile(testFileContent))
    }

  }

  @Test def testIndexRetrieverWithProbe() {
    val indexFile = "testIndexFile"
    val baseDir = "edgar-daily"
    val testFilePath = s"$baseDir/$indexFile"
    val testFileContent = "|CIX23|20911|4|COMPANY DATA|EDGAR/data/files"
    val mockFtpClient = createMockFtpClient(testFilePath, indexFile)
    when(mockFtpClient.listDirectory(baseDir)).thenReturn(List(indexFile))
    
    val expectedFinalMsg = FileContent(testFileContent)
    val downloaderProbe = TestProbe() 
    val indexProcessor = TestProbe()
                
    val retriever = TestActorRef(Props(classOf[IndexRetrieverActor], 
                                  indexProcessor.ref,
                                  downloaderProbe.ref,
                                  mockFtpClient,
                                  baseDir))

    within(1000 millis) {
      retriever ! DownloadLatestIndex
      downloaderProbe.expectMsg(1000 millis, DownloadFile(testFilePath))
      downloaderProbe.reply(expectedFinalMsg)
      indexProcessor.expectMsg(1000 millis, ProcessIndexFile(testFileContent))
    }

  }

  @Test  def testFileSink() {
    val mockEdgarSink =  Mockito.mock(classOf[EdgarSink])
    val sink = TestActorRef(Props(classOf[EdgarFileSinkActor], mockEdgarSink))
    val testString = "<ownershipDocument></ownershipDocument></XML>"
    within(2000 millis) {
      sink ! FilingInfo(testString)
      expectNoMsg
    }

  }

  @Test  def testChildDownloaders() {
    val testFilePath = "/test/filePath"
    val testFileContent = "testFileContent"
    val mockFtpClient = createMockFtpClient(testFilePath, testFileContent)
    
    val mockSender = TestActorRef(new Actor {
      def receive = {
        case _ => SimpleFilingFinished("foo", null)
      }
    })
    val childDownloader = TestActorRef(Props(classOf[ChildDownloader], mockFtpClient))
    within(2000 millis) {
      childDownloader ! Download(testFilePath, mockSender)
      expectMsg(SimpleFilingFinished(testFileContent, mockSender))
    }

  }

  @Test  def testIndexProcessorActorWithProbe() {
    val testFileContent = "CIX23|20911|4|COMPANY DATA|EDGAR/data/files"
    val fileLines = testFileContent.split('|')
    val filteredFiles = List(fileLines).map(arr => EdgarFiling(arr(0), arr(3), 
                                                                    arr(2), arr(1),
                                                                    arr(4)))
    
    val mockIndexProcessor = Mockito.mock(classOf[IndexProcessor])
    when(mockIndexProcessor.processIndexFile(testFileContent)).thenReturn(filteredFiles)
    
    val inputMessage = ProcessIndexFile(testFileContent)
    val edgarFileManager = TestProbe() 
    val expectedFMgrMessage = FilteredFiles(filteredFiles)
    
    val indexProcessorActor = TestActorRef(Props(classOf[IndexProcessorActor],
                                  mockIndexProcessor,
                                  edgarFileManager.ref))
    within(1000 millis) {
      indexProcessorActor ! inputMessage
      edgarFileManager.expectMsg(1000 millis, FilteredFiles(filteredFiles))
    }
    Mockito.verify(mockIndexProcessor).processIndexFile(testFileContent)
  }
  

  @Test  def testEdgarFileManagerForFileContentMessageToSink() {
    val testFileContent = "<xmlContent>Test</xmlContent>"
    val downloaderProbe = TestProbe() 
    val edgarSinkProbe = TestProbe()
                
    val edgarFileManager = TestActorRef(Props(classOf[EdgarFileManager], 
                                  downloaderProbe.ref,
                                  edgarSinkProbe.ref))

    within(1000 millis) {
      edgarFileManager ! FileContent(testFileContent)
      edgarSinkProbe.expectMsg(1000 millis, FilingInfo(testFileContent))
    }

  }
  
  @Test  def testEdgarFileManagerForStreamContentMessageToSink() {
    val testFileContent:EdgarTypes.XBRLFiling = List(("fileName", "content"))
    val downloaderProbe = TestProbe() 
    val edgarSinkProbe = TestProbe()
                
    val edgarFileManager = TestActorRef(Props(classOf[EdgarFileManager], 
                                  downloaderProbe.ref,
                                  edgarSinkProbe.ref))

    within(1000 millis) {
      edgarFileManager ! StreamContent(testFileContent)
      edgarSinkProbe.expectMsg(1000 millis, FilingXBRLInfo(testFileContent))
    }

  }

  @Test def testEdgarFileManagerWorkflow() {
    val testFileContent = "<xmlContent>Test</xmlContent>"
    val testFilingFile = "testFilingFile"
    val testEdgarFilings = EdgarFiling("cik", "asOfDate", "4",
                                    "companyName", testFilingFile)
    val filteredFilesMsg = FilteredFiles(List(testEdgarFilings))
    val downloaderProbe = TestProbe() 
    val edgarSinkProbe = TestProbe()
    
    val shutdownProbe = TestProbe()
    shutdownProbe watch downloaderProbe.ref
    
    val edgarFileManager = TestActorRef(Props(classOf[EdgarFileManager], 
                                  downloaderProbe.ref,
                                  edgarSinkProbe.ref))

    within(2000 millis) {
      edgarFileManager ! filteredFilesMsg
      downloaderProbe.expectMsg(1000 millis, DownloadFile(testFilingFile))
      downloaderProbe.reply(FileContent(testFileContent))
      edgarSinkProbe.expectMsg(1000 millis, FilingInfo(testFileContent))
      
    }
    shutdownProbe.expectTerminated(downloaderProbe.ref);

  }
  
  @Test  def testDownloadManager() {
    val testFileName = "testFileName"
    val testFileContent = "testFileContent"
    
    object fakeFtpFactory extends DefaultFactory {
      def edgarFtpClient(password:String):FtpClient = createMockFtpClient(testFileName, testFileContent)
      def indexProcessor(filterFunction:Array[String]=>Boolean) = null
      def edgarSink = null
    }

    val testFtpFactory = fakeFtpFactory
    val downloadManager = TestActorRef(Props(classOf[DownloadManager], 
                                  1, testFtpFactory))

    within(2000 millis) {
      downloadManager ! DownloadFile(testFileName)
      expectMsg(FileContent(testFileContent))
    }

  }
}