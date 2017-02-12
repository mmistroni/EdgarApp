package edgar.util
import net.liftweb.json.DefaultFormats
import net.liftweb.json._
import scala.io._
import scala.io.Source
import scala.util.parsing.json.JSON

case class DirContent(name:String, href:String, `type`:String)
    

/**
 * Craw a directory until it reaches the last
 * node
 */
trait WebCrawler extends edgar.util.LogHelper {
  
  implicit val formats = DefaultFormats
    
  private def extractJson(documentText:String):Seq[DirContent] = {
    logger.info("Extracting JSON....")
    JSON.parseFull(documentText)
    val json = parse(documentText)
    val elements = (json \\ "directory" \\ "item").children
    
    val items = elements.map(item => item.extract[DirContent])
    logger.info(s" we got:${items.size}")
    items
  }
  
  def crawlDirectory(startDirectory:String):Seq[String] = {
    logger.info(s"Crawler.Parsing directory:$startDirectory")
    val indexFile= s"$startDirectory/index.json"
    val response = Source.fromURL(indexFile).mkString
    val dirContents = extractJson(response)
    val directories = dirContents.filter(file => file.`type` == "dir")
    
    if (directories.isEmpty) {
      dirContents.filter(file => file.`type` == "file")
                 .filter(file => file.href.indexOf("master")>=0).map(item => s"$startDirectory/${item.href}")
    } else {
      val latest = directories.last.name
      logger.info(s"Last file is :$latest")
      val nextPath = startDirectory + latest + "/"
      
      crawlDirectory(nextPath)
    }
 }
  
}