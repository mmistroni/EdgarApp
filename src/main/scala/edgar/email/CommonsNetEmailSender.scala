package edgar.email
import edgar.util.LogHelper

trait CommonsNetEmailSender extends LogHelper {
  import org.apache.commons.mail._

  val mailConfigProperties: EmailConfig

  def createEmail = new HtmlEmail()

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
    email.setHtmlMsg(content)
    email.addTo(recipients: _*)
  }

  def sendMail(subject: String, content: String, recipients: String*): Unit = {
    val msg = buildEmail(subject, content, recipients: _*)
    logger.info("sending email...")
    logger.info(msg.getToAddresses)
    msg.send();
  }
}
