import edgar.email._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.junit._
import scala.io._
import Assert._

import org.mockito.{ Mockito, Matchers }
import org.apache.commons.mail._

import javax.mail._
import javax.mail.internet._
import java.util.Date
import java.util.Properties
import scala.collection.JavaConversions._
import org.powermock.api.mockito.PowerMockito._
import org.junit._

class EmailTestSuite { //extends FunSuite {
  val testUsername = "testuser"
  val testPassword = "testpassword"
  val testHost = "testHost"
  val testFromAddress = "from@gmail.com"
  val testPort = -1
  val mockMimeMessage = Mockito.mock(classOf[Message])
  
  
  
  def createMockConfig = {
    val config = new EmailConfig {
      val username = "mmistroni@gmail.com"
      val password = "80t5w4n4;"
      val host = "smtp.gmail.com"
      val port = 587
      val fromAddress  = testFromAddress
    }
    config
  }

  class MockEmailSender extends EmailSender {
    val mailConfigProperties = createMockConfig

    override def _createMimeMessage = mockMimeMessage
  }

  object MockEmail extends SimpleEmail
  
  @Test @Ignore def testBuildEmail() {
    val recipients = Seq("test@gmail.com")
    val content = "content"
    val subject = "subject"

    val sender = new MockEmailSender()
    val msg = sender.buildMessage(subject, content, recipients)

    val expectedArgType = classOf[Array[Address]]

    Mockito.verify(mockMimeMessage, Mockito.times(1)).addFrom(Matchers.any())
    Mockito.verify(mockMimeMessage, Mockito.times(1)).setSubject(subject)
    Mockito.verify(mockMimeMessage, Mockito.times(1)).setContent(content, "text/html")
    
  }
  
  @Test @Ignore
  def sendEmailUsigCommon() {
    
    val config = new EmailConfig {
      val username = "mmistroni@gmail.com"
      val password = "80t5w4n4;"
      val host = "smtp.gmail.com"
      val port = 587
      val fromAddress  = testFromAddress
    }
    
    val sender = new CommonsNetEmailSender{
      val mailConfigProperties  =config
    }
    
    sender.sendMail("TestMail from apache net", "this is a test", "mmistroni@gmail.com")
  }
  
  @Test 
  def testBuildMailUsingCommonsNetEmailSender() {
    val mockEmail = Mockito.mock(classOf[SimpleEmail])
    val testConfigProperties = createMockConfig
    val recipients = "test@gmail.com"
    val content = "content"
    val subject = "subject"

    val sender = new CommonsNetEmailSender {
      val mailConfigProperties= testConfigProperties
      override def createEmail = mockEmail
    }
    
    val email = sender.buildEmail(subject, content, recipients)
    
    Mockito.verify(mockEmail, Mockito.times(1)).setHostName(testConfigProperties.host)
    Mockito.verify(mockEmail, Mockito.times(1)).setSmtpPort(testConfigProperties.port)
    Mockito.verify(mockEmail, Mockito.times(1)).setFrom(testConfigProperties.fromAddress)
    Mockito.verify(mockEmail, Mockito.times(1)).setSubject(subject)
    Mockito.verify(mockEmail, Mockito.times(1)).setMsg(content)
    
  }
  
  @Test 
  def testSendMailUsingCommonsNetEmailSender() {
    val sendResult = "email sent!"
    class MockEmail extends SimpleEmail {
      var result = ""
      override def send():String = {
        result = sendResult
        sendResult
      }
    }
    val mockEmail = new MockEmail()
    val testConfigProperties = createMockConfig
    val recipients = "test@gmail.com"
    val content = "content"
    val subject = "subject"

    val sender = new CommonsNetEmailSender {
      val mailConfigProperties= testConfigProperties
      override def createEmail = mockEmail
    }
    
    sender.sendMail(subject, content, recipients)
    assertEquals(sendResult, mockEmail.result)
    
  }
  
  
  
  
  
  
}