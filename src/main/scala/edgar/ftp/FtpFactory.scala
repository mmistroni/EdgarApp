package edgar.ftp

trait FtpFactory {
  def edgarFtpClient(baseDir: String): FtpClient = new HttpsFtpClient(baseDir)
}