package edgar.core
import edgar.email.{ CommonsNetEmailSender, EmailConfig }
import amazon.util.AWSClient
import com.typesafe.config.Config

/**
 * This module needs an unit test!!!
 */

class S3Sink(appConfig: Config) extends OutputStreamSink with AWSClient {
  private [core] val bucketName = "edgar-bucket-mm"
  private [core] val topicName = "arn:aws:sns:us-west-2:049597339122:EdgarSMSPublisher"
  private [core] val emailTopicName = "arn:aws:sns:us-west-2:049597339122:SharesNotificationList"
  
  
  import edgar.util.HtmlTableGenerator._

  override def emptySink = {
    super.emptySink
    
    if (this.securitesMap.size > 0) {
      logger.info("Sending Content to:" + appConfig.getString("smtp.recipients"))
      val content = generateHtmlTable(this.securitesMap)
      storeSecuritiesFile
      publishToRecipients(content)
    } else {
      logger.info(s"Found no securities in ${createFileName}. Notifying clients..")
      snsClient.publishToTopic(topicName, "Edgar S3 Upload", "no content found")
    }
  }
  
  def createFileName = {
    import java.text._
    val fileTimestamp = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date())
    s"data/$fileTimestamp-securites.txt"
    
  }
  
  def publishToRecipients(emailContent:String) = {
    val recipients  = appConfig.getString("smtp.recipients").split(",").toSeq
    logger.info("Dropping messag to SESClient")
    sesClient.sendEmail(recipients, "EdgarInstitutionalInvestors", emailContent)  
  }
  
  
  def createFileContent = {
    if (securitesMap.size > 0) securitesMap.values.flatten.mkString("\n") else ""
  }

  def storeSecuritiesFile = {
    
    val securitiesString = createFileContent
    
    if (securitiesString.length > 0) {
      val s3Sink = s3Client
      logger.info(s"Now attempting to store data in S3. Size of the data is:${securitiesString.length}")
      s3Sink.storeOnS3(bucketName, createFileName, securitiesString)
    } else {
      logger.info(s"Found no securities in ${createFileName}. Notifying clients..")
      val snsSink = snsClient
      snsClient.publishToTopic(topicName, "Edgar S3 Upload", "no content found")
    }
    logger.info("Outta here...")

  }

}
  
  