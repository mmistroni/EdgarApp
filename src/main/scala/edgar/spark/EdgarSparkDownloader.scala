package edgar.spark

import edgar.ftp._
import edgar.core._
import scala.xml._

import org.apache.spark._

/**
 * This module uses Spark to download edgar content . Maybe not the best idea for a pipeline, 
 * but it's just for testing spark 
 * What we want here is to download the XML file and do something about it..
 * Perhaps a better idea would be to launch the EdgarActorRunner to store processed XML file
 * into a directory that will be read by spark Streaming perhaps???
 * At the  moment what this do is retrieve Edgar Filing as a String and display it to output
 * Run this module like this:
 * 
 * spark-submit --class edgar.spark.EdgarSparkDownloader <path to jar file>
 * 
 * 
 */

case class Form4Filing(issuerName:String, issuerCik:String, reportingOwnerCik:String,
                       transactionCode:String)
    
object EdgarSparkDownloader {
  
  def downloadFtpFile(fileName:String):String = {
    val factory  =EdgarFactory
    val ftpClient = factory.edgarFtpClient(java.util.UUID.randomUUID().toString() + "@downloader.com")
    ftpClient.retrieveFile(fileName)
  }
  
  def parseXmlFile(fileContent:String):Form4Filing = {
   val content = fileContent.substring(fileContent.indexOf("?>") + 2, fileContent.indexOf("</XML"))
   val xml = XML.loadString(content)
      val formType = xml \\ "submissionType"
      val issuerName = xml \\ "issuerName"
      val issuerCik = xml \\ "issuerCik"
      val reportingOwnerCik = xml \\ "rptOwnerCik"
      val transactionCode = xml \\ "transactionCode"
      Form4Filing(issuerName.text, issuerCik.text, reportingOwnerCik.text, transactionCode.text) 
  }
  
  def processFiles():Unit = {
    val conf = new SparkConf().setAppName("Simple Application")
    val sc = new SparkContext(conf)
    val lines = sc.textFile("file:///c:/Users/marco/testsbtproject/masterq2.gz", 5)
    
    val filtered = lines.map(l => l.split('|')).filter(arr=> arr.length > 2).map(arr => (arr(2), arr(4))).zipWithIndex

    val noHeaders = filtered.filter( tpl => tpl._2 > 0).map(tpl => tpl._1).filter(tpl => tpl._1 == "4").map(_._2)
    
    noHeaders.take(10).foreach(println)
    
    noHeaders.cache()
    println("Now Fetching.....")
    val fin = noHeaders.map(fileName => downloadFtpFile(fileName)).map(parseXmlFile)
    fin.cache()
    
    //println("Finidng who bought shares")
    //fin.filter(form4=> form4.transactionCode.trim().equals("A")).foreach(println)
    println("Finding who bought shars in own company")
    println(fin.filter(form4=> form4.issuerCik.trim().equals(form4.reportingOwnerCik.trim())).count())
    
    
  }
  
  def main(args: Array[String]) {
    processFiles
  } 
}