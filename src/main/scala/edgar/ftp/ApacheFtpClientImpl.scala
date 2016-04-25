package edgar.ftp

import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTP._
import org.apache.commons.net.ftp.FTPReply
import java.io.InputStream
import org.apache.commons.io.IOUtils

import edgar.util.LogHelper

class ApacheFTPClientImpl(_username: String, _password: String, _host: String) extends ApacheFTPClient with LogHelper {
    val ftpConfig = new FtpConfig {
    val username = _username
    val password = _password
    val host = _host
  }

  override def readStream(is: InputStream): String = {
    println(is.getClass().getName());

    IOUtils.toString(is, "UTF-8")
  }

}
