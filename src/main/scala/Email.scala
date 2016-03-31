
import edgar.ftp.FtpConfig
import javax.mail._
import javax.mail.internet._
import java.util.Date
import java.util.Properties
import scala.collection.JavaConversions._

package edgar.mail {


  trait EmailConfig extends FtpConfig {
    val port: Int
    val fromAddress:String
    
    
    def toProperties = {
      val mailProps = new Properties()
      mailProps.put("mail.smtp.host", this.host)//"smtp.gmail.com");
      mailProps.put("mail.smtp.port", String.valueOf(this.port))//"587");
      mailProps.put("mail.smtp.auth", "true");
      mailProps.put("mail.smtp.starttls.enable", "true");
      mailProps.put("mail.smtp.user", this.username);
      mailProps.put("mail.smtp.password", this.password)//"80t5w4n4;");
      mailProps.put("mail.debug", "true")
      mailProps
    }
    
  }
  
  trait CommonsNetEmailSender extends edgar.core.LogHelper {
    import org.apache.commons.mail._
    
    val mailConfigProperties : EmailConfig
    
    def createEmail = new SimpleEmail()
    
    def buildEmail(subject: String, content: String, recipients: String*) = {
      val email = createEmail
      email.setHostName(mailConfigProperties.host);
      email.setSmtpPort(mailConfigProperties.port);
      email.setDebug(true)
      email.setAuthenticator(new DefaultAuthenticator(mailConfigProperties.username, 
                             mailConfigProperties.password))
      email.setSSLOnConnect(true)
      email.setFrom(mailConfigProperties.fromAddress)
      email.setSubject(subject)
      email.setMsg(content)
      email.addTo(recipients:_*)
    }
    
    def sendMail(subject: String, content: String, recipients: String*): Unit = {
      val msg = buildEmail(subject, content, recipients:_*)
      logger.info("sending email...")
      logger.info(msg.getToAddresses)
      msg.send();
    }
  }
  
  
  trait EmailSender extends edgar.core.LogHelper{
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
}