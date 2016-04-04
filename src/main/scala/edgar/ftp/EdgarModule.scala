package edgar.ftp

trait EdgarModule {
  val ftpClient: FtpClient

  def list(dirName: String): List[String] = {
    ftpClient.listDirectory(dirName)
  }

  def downloadFile(fileName: String): String = {
    ftpClient.retrieveFile(fileName)
  }
}
