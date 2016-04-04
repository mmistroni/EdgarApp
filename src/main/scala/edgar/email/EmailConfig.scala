package edgar.email
import edgar.ftp.FtpConfig
import java.util.Properties

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