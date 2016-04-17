package edgar.spark

import edgar.ftp._
import edgar.core._

import org.apache.spark._


object EdgarSparkDownloader {
  
  def downloadFtpFile(fileName:String):String = {
    val factory  =EdgarFactory
    val ftpClient = factory.edgarFtpClient(java.util.UUID.randomUUID().toString() + "@downloader.com")
    ftpClient.retrieveFile(fileName)
  }
  
  
  def processFiles():Unit = {
    val conf = new SparkConf().setAppName("Simple Application")
    val sc = new SparkContext(conf)
    val lines = sc.textFile("file:///c:/Users/marco/testsbtproject/sample.master.idx")
    
    val filtered = lines.map(l => l.split('|')).filter(arr=> arr.length > 2).map(arr => arr(4)).zipWithIndex

    val noHeaders = filtered.filter( tpl => tpl._2 > 0).map(tpl => tpl._1)
    
    noHeaders.take(10).foreach(println)
    
    println("Now Fetching.....")
    val mapped = noHeaders.take(10).map(fileName => downloadFtpFile(fileName)).foreach(println)
    
    
  }
  
  def main(args: Array[String]) {
    processFiles
  } 
}