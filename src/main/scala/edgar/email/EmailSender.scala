package edgar.email
import javax.mail._
import javax.mail.internet._
import java.util.Date
import java.util.Properties
import scala.collection.JavaConversions._

import edgar.util.LogHelper

trait EmailSender extends LogHelper{
    val mailConfigProperties : EmailConfig
    
    protected def getSession():Session = {
       Session.getInstance(mailConfigProperties.toProperties,
		  new javax.mail.Authenticator() {
			override def getPasswordAuthentication():PasswordAuthentication =  {
				return new PasswordAuthentication(mailConfigProperties.username, 
				                                  mailConfigProperties.password);
			}
		  });
    }

    protected def _createMimeMessage:Message = new MimeMessage(getSession()) 

    
    def buildMessage(subject:String, content:String, recipients:Seq[String]):Message = {
      val mimeMessage = _createMimeMessage
      mimeMessage.addFrom(createAddresses(List(mailConfigProperties.fromAddress)))
      mimeMessage.setSubject(subject)
      mimeMessage.setContent(content, "text/html");
      mimeMessage.setRecipients(Message.RecipientType.TO,
                                createAddresses(recipients))
      mimeMessage
      
    }
    
    def sendMail(subject: String, content: String, recipients: (String)*): Unit = {
      val msg = buildMessage(subject, content, recipients)
      logger.info("sending email...")
      logger.info(msg.getRecipients(Message.RecipientType.TO))
      send(msg)
    }
    
    protected def send(message:Message) = {
      Transport.send(message)
    }
    
    
    private def createAddresses(recipients:Seq[String]):Array[Address] = {
       val addresses = for(recipient <- recipients) yield {new InternetAddress(recipient)}
       addresses.toArray
    }
    
  }