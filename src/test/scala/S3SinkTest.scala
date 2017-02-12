import edgar.core._
import org.junit._
import scala.io._
import org.junit.Assert._
import org.mockito.Mockito
import org.apache.commons.mail._
import javax.mail._
import javax.mail.internet._
import scala.collection.JavaConversions._
import org.powermock.api.mockito.PowerMockito._
import org.junit._
import amazon.util.{AWSClient, S3Client ,SNSClient, SESClient}
import edgar.core.S3Sink
import com.typesafe.config.Config
import amazon.util.S3Client
import com.typesafe.config.Config
import edgar.email.EmailConfig
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.junit._


@RunWith(classOf[JUnitRunner])
class S3SinkTestSuite extends org.scalatest.FunSuite {
  
  val mockS3Client = Mockito.mock(classOf[S3Client])
  val mockSnsClient = Mockito.mock(classOf[SNSClient])
  val mockSesClient = Mockito.mock(classOf[SESClient])
  val mockConfig = Mockito.mock(classOf[EmailConfig])
  val appConfig = Mockito.mock(classOf[Config])
  val testFileName = "testFileName"
  val testBucketName = "testBucketName"
  val testFileContent  ="testFileContent"
  val testTopicName = "testTopic"
  
  trait MockAmazonClient extends AWSClient {
    override def s3Client = mockS3Client
    override def snsClient = mockSnsClient
    override def sesClient = mockSesClient
    
  }
  
  class MockSink(fileContent:String) extends S3Sink(appConfig) with MockAmazonClient {
    override val bucketName = testBucketName
    override val topicName = testTopicName
    override def createFileName = testFileName
    override def createFileContent = fileContent
  }
  
  
  def createMockSink(fileContent:String) = new MockSink(fileContent)
  
  
  
  test("store securities file when there are securities ") {
    assertTrue(true) 
    val s3Sink = new MockSink(testFileContent)
    
    s3Sink.storeSecuritiesFile
    Mockito.verify(mockS3Client).storeOnS3(testBucketName, testFileName, testFileContent)
  
    
    
  }
  
  test("store securities file when there are no securities ") {
    val s3Sink = new MockSink("")
    s3Sink.storeSecuritiesFile
    Mockito.verify(mockSnsClient).publishToTopic(testTopicName, "Edgar S3 Upload", "no content found")
    
  }
   
  test("publish message to ses client") {
    val testRecipients = "abc,def"
    val s3Sink = new MockSink("")
    val testContent = "testContent"
    Mockito.when(appConfig.getString("smtp.recipients")).thenReturn(testRecipients)
    
    val expectedRecipients = testRecipients.split(",").toSeq
    s3Sink.publishToRecipients(testContent)
    Mockito.verify(mockSesClient).sendEmail(expectedRecipients, 
                                            "EdgarInstitutionalInvestors", testContent)
    
    
    
  }
  
  
  
}