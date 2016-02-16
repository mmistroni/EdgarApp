
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
  

  @Test @Ignore
  def testDownloaderSynchronously() {

    val downloader = TestActorRef[ChildDownloader]

    implicit val timeout = Timeout(5 seconds)
    val future = downloader ? DownloadFile("Foo")

    val result = Await.result(future, 1 second)
    assertEquals(FileContent("|CIX23|20911|4|COMPANY DATA|EDGAR/data/files"), result)

  }
  /** val retriever = TestActorRef(Props(classOf[IndexRetriever], downloader)) val sink = TestActorRef[EdgarFileSink] val sink2 = system.actorOf(Props[EdgarFileSink]) val fileManager = TestActorRef(Props(classOf[EdgarFileManager], downloader, sink)) val processor = TestActorRef(Props(classOf[IndexProcessor], fileManager)) val master = TestActorRef(Props(classOf[EdgarMaster], retriever, processor, fileManager)) **/

  @Test @Ignore
  def testDownloaderAsync() {

    val downloader = TestActorRef[ChildDownloader]

    within(1000 millis) {
      downloader ! DownloadFile("Test")
      expectMsg(FileContent("|CIX23|20911|4|COMPANY DATA|EDGAR/data/files"))
    }

  }

  @Test @Ignore def testRetriever() {

    val downloader = TestActorRef[ChildDownloader]
    val retriever = TestActorRef(Props(classOf[IndexRetrieverActor], downloader))

    within(1000 millis) {
      retriever ! DownloadLatestIndex
      expectMsg(FileContent("|CIX23|20911|4|COMPANY DATA|EDGAR/data/files"))
    }

  }

  @Test def testFileSink() {
    val sink = TestActorRef[EdgarFileSink]
    val testString = "<ownershipDocument></ownershipDocument></XML>"
    within(2000 millis) {
      sink ! FilingInfo(testString)
      expectNoMsg
    }

  }

  @Test def testChildDownloadersFileSink() {
    val testFilePath = "/test/filePath"
    val testFileContent = "testFileContent"
    val mockFtpClient = createMockFtpClient(testFilePath, testFileContent)
    
    val mockSender = TestActorRef(new Actor {
      def receive = {
        case _ => Finished("foo", null)
      }
    })
    val childDownloader = TestActorRef(Props(classOf[ChildDownloader], mockFtpClient))
    within(2000 millis) {
      childDownloader ! Download(testFilePath, mockSender)
      expectMsg(Finished(testFileContent, mockSender))
    }

  }

  
  @Test @Ignore def testIndexRetrieverWithProbe() {
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

  @Test def testIndexProcessorActorWithProbe() {
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
  
  
  // TODO
  /**
   * Test following actors
   * DownloadManager
   * EdgarManager
   */

}