package edgar.email
import edgar.util.LogHelper

trait CommonsNetEmailSender extends LogHelper {
  import org.apache.commons.mail._

  val mailConfigProperties: EmailConfig

  def createEmail = new HtmlEmail()

  def buildEmail(subject: String, content: String, recipients: String) = {
    val email = createEmail
    email.setHostName(mailConfigProperties.host);
    email.setSmtpPort(mailConfigProperties.port);
    email.setDebug(true)
    email.setAuthenticator(new DefaultAuthenticator(mailConfigProperties.username,
      mailConfigProperties.password))
    email.setSSLOnConnect(true)
    email.setFrom(mailConfigProperties.fromAddress)
    email.setSubject(subject)
    email.setHtmlMsg(content)
    val allRecipients:Array[String] = recipients.split(",")
    logger.info("Sending mail to " + allRecipients.mkString(":"))
    email.addTo(allRecipients: _*)
  }

  def sendMail(subject: String, content: String, recipients: String): Unit = {
    logger.info("EMail sender, sending email to:" + recipients)
    
    val msg = buildEmail(subject, content, recipients)
    logger.info(msg.getToAddresses)
    msg.send();
  }
}
