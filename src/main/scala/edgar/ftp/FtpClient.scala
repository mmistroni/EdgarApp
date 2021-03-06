package edgar.ftp

trait FtpClient {
  val ftpConfig: FtpConfig
  def listDirectory(dirName: String): Seq[String]
  def retrieveFile(fileName: String): String
  def retrieveZippedStream(fileName: String): List[(String, String)]
  def disconnect:Unit
}