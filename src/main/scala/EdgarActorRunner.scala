

import akka.actor._

import edgar.actors._
import edgar.actors.EdgarRequests._
import edgar.predicates.EdgarPredicates._
import edgar.ftp._
import edgar.core._
import edgar.email._
import edgar.predicates.EdgarPredicates.or
import java.util.UUID

object EdgarActorRunner extends App with edgar.util.LogHelper {

  logger.info("Starting the Actor System....")
  val filterFunction = formType2In(Seq("13F-HR"))
  val factory = EdgarFactory
    
  def createFilterFunction():EdgarFilter = {
    val cikFilter = cikIn(Set("886982", "19617", "1067983"))     // GS, JPM. BRKB)
    val includeFormTypesFilter = formTypeIn(Set("13F-HR"))
    val excludeFormTypesFilter = excludeFormTypes(List("424B2", "8-K"))
    val sameCikFilter  = and(Seq(cikFilter, excludeFormTypesFilter))_
    //or(Seq(sameCikFilter, includeFormTypesFilter ))_
    includeFormTypesFilter
  }
  
  
  def launchActorSystem = {
    
    import com.typesafe.config.ConfigFactory
    // load sys properties required for email sink
    val conf = ConfigFactory.load()

    val system = ActorSystem("Edgar-Filings-Downloader")
    val downloader =
      system.actorOf(Props(classOf[DownloadManager], 3, factory), "DownloadManager")
     
    
    val config = new EmailConfig {
      override val username = conf.getString("smtp.username")
      override val password = conf.getString("smtp.password")
      override val host = conf.getString("smtp.host")
      override val port = conf.getInt("smtp.port")
      override val fromAddress  = "noreply@worlcorpservices.com"
    }  
    
    val emailSink = new OutputStreamSink with CommonsNetEmailSender {
      override val mailConfigProperties = config
      import edgar.util.HtmlTableGenerator._
      
      override def emptySink = {
        logger.info("MyEmailSink. calling super empty sink")
        super.emptySink
        logger.info("And now displaying mail properties..")
        logger.info(mailConfigProperties.toProperties)
        logger.info("Sending Content..")
        val content = generateHtmlTable(this.securitesMap)
        this.sendMail("Edgar Institutional Investor Securities", conf.getString("smtp.recipients"))
      }
    }
      
    val edgarFileSink = system.actorOf(Props(classOf[EdgarFileSinkActor], 
        emailSink
        ), "EdgarFileSink")
    val edgarFileManager = system.actorOf(Props(classOf[EdgarFileManager],
      downloader, edgarFileSink), "EdgarFileManager")
  
    val indexProcessor = system.actorOf(Props(classOf[IndexProcessorActor],
      new IndexProcessorImpl(filterFunction),
      edgarFileManager), "IndexProcessor")
  
    val indexRetriever = system.actorOf(Props(classOf[IndexRetrieverActor],
      indexProcessor, downloader, factory.edgarFtpClient(UUID.randomUUID().toString() + "@downloader.com"),
      "edgar/daily-index"), "IndexRetriever")
  
    val master = system.actorOf(Props(classOf[EdgarMaster],
      indexRetriever, downloader,
      indexProcessor,
      edgarFileManager), "Master")

    master ! Start
  }
  
  launchActorSystem
  

}