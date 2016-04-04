package edgar.core
import edgar.predicates.EdgarPredicates._
import edgar.ftp.FtpFactory

abstract class DefaultFactory {
  import edgar.ftp.FtpClient

  def edgarSink: EdgarSink

  def indexProcessor(filterFunction: EdgarFilter): IndexProcessor

  def edgarFtpClient(password: String): FtpClient
}

object EdgarFactory extends DefaultFactory with FtpFactory {

  def edgarSink() = new OutputStreamSink {}
  def indexProcessor(filterFunction: EdgarFilter) = new IndexProcessorImpl(filterFunction)
}
