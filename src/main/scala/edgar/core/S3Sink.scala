package edgar.core
import edgar.email.{ CommonsNetEmailSender, EmailConfig }
import amazon.util.AWSClientFactory
import com.typesafe.config.Config

/**
 * This module needs an unit test!!!
 */

class S3Sink(config: EmailConfig, appConfig: Config) extends OutputStreamSink with CommonsNetEmailSender {
  override val mailConfigProperties = config
  private val bucketName = "edgar-bucket-mm"
  private val s3Client = AWSClientFactory.s3Client(appConfig.getString("aws.accessKey"),
    appConfig.getString("aws.secretKey"))

  import edgar.util.HtmlTableGenerator._

  override def emptySink = {
    logger.info("MyEmailSink. calling super empty sink")
    super.emptySink
    logger.info("And now displaying mail properties..")
    logger.info(mailConfigProperties.toProperties)
    logger.info("Sending Content to:" + appConfig.getString("smtp.recipients"))
    val content = generateHtmlTable(this.securitesMap)

    sendMail("Edgar Institutional Investor Securities", content, appConfig.getString("smtp.recipients"))

    storeSecuritiesFile
  }

  def storeSecuritiesFile = {
    import java.text._
    val fileTimestamp = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date())
    val fileName = s"data/$fileTimestamp-securites.txt"
    val securitiesString = securitesMap.values.flatten.mkString("\n")
    logger.info("Now attempting to store data in S3")
    s3Client.storeOnS3(bucketName, fileName, securitiesString)
    logger.info("Outta here...")

  }

}
  
  