package edgar.core
import edgar.util.LogHelper
import edgar.predicates.EdgarPredicates._

trait IndexProcessor {
  def processIndexFile(fileContent: String): Seq[EdgarFiling]
}

class IndexProcessorImpl(filterFunction: EdgarFilter) extends IndexProcessor with LogHelper {

    def processIndexFile(content: String): Seq[EdgarFiling] = {
      logger.info("Procesing index file...")
      val lines = content.split("\n").toList.map(ln => ln.split('|')).filter(arr => arr.size > 2)
        .map(arr => EdgarFiling(arr(0), arr(3),
          arr(2), arr(1),
          arr(4)))
      // temporarily limiting to 600 entries until FTP issues are resolved
      val res = lines.filter(filterFunction)
      logger.info("Outpupt lines are:" + res.size)
      res
    }

  }
