package edgar.core
import edgar.email.{ CommonsNetEmailSender, EmailConfig }
import amazon.util.AWSClient
import com.typesafe.config.Config

/**
 * This module needs an unit test!!!
 */


class SNSSink(appConfig: Config)  extends AWSClient with edgar.util.LogHelper{
  def notify(topicName:String, subject:String, messageContent:String) = {
    snsClient.publishToTopic(topicName, subject, messageContent) 
  }
  
}
  
  