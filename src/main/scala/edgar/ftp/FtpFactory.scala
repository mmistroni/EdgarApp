package edgar.ftp

trait FtpFactory {
  def edgarFtpClient(_password: String): FtpClient =
    new ApacheFTPClient {
      val ftpConfig = new FtpConfig {
        val username = "anonymous"
        val password = _password
        val host = "ftp.sec.gov"
      }
    }

}