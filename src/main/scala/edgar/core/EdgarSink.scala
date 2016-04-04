package edgar.core
import edgar.util.LogHelper
import scala.xml._

trait EdgarSink {

  def storeFileContent(fileContent: EdgarTypes.SimpleFiling)

  def storeXBRLFile(xbrl: EdgarTypes.XBRLFiling)

  def emptySink()
}
trait OutputStreamSink extends EdgarSink with LogHelper {
  var messageList = new scala.collection.mutable.ListBuffer[(String, Seq[String])]()

  def storeFileContent(fileContent: EdgarTypes.SimpleFiling) = {
    if (fileContent.indexOf("<informationTable") >= 0) {
      val xmlContent = fileContent.substring(fileContent.indexOf("<edgarSubmission"), fileContent.indexOf("</XML"))
      val xml = XML.loadString(xmlContent)
      val formType = xml \\ "submissionType"
      val issuerName = xml \\ "issuerName"
      val issuerCik = xml \\ "issuerCik"
      val reportingOwnerCik = xml \\ "rptOwnerCik"
      val filingManager = xml \\ "filingManager" \\ "name"
      logger.info(s"FileSink|$formType.text|$issuerName.text|$issuerCik.text|$reportingOwnerCik.text|$filingManager.text")
      logger.debug(fileContent.indexOf("<informationTable"))
      logger.debug(fileContent.indexOf("</informationTable"))
      val informationTable = fileContent.substring(fileContent.indexOf("<informationTable"),
        fileContent.indexOf("</informationTable>") + 20)
      logger.debug(informationTable)
      val infoTableXml = XML.loadString(informationTable)
      val purchasedShares = infoTableXml \\ "nameOfIssuer"
      val holdingSecurities = infoTableXml \\ "nameOfIssuer"
      holdingSecurities.foreach(iss => logger.debug(iss.text))
      messageList.append((filingManager.text, holdingSecurities.map(_.text)))

    } else if (fileContent.indexOf("<?xml version") >= 0) {
      val xmlStart = fileContent.substring(fileContent.indexOf("?>") + 2, fileContent.indexOf("</XML"))
      logger.debug("Generic XMl:\n" + xmlStart)
    } else {
      val formType = fileContent.substring(fileContent.indexOf("<TYPE>") + 6, fileContent.indexOf("<SEQUENCE")).trim();
      logger.debug("Invalid content for forMTYPE:" + formType)
    }
  }

  def storeXBRLFile(fileList: EdgarTypes.XBRLFiling) = {
    val (first, firstContent) = fileList.head
    logger.info(s"Content for $first is :\n$firstContent")
  }

  def emptySink = {
    logger.info("We hsould send all informations we have collected..")
    logger.info(this.messageList)
  }

}
