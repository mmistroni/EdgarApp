package edgar.util


object HtmlTableGenerator {
  
  def generateHtmlTable(inputMap:scala.collection.mutable.Map[String, Seq[String]]):String = {
    val sb = new StringBuilder("<html><body><table bgcolor=\"#B6EBF\" border=\"3\" cellspacing=\"4\">")
            .append("<th>InstitutionalManager</th><th>Securities Held</th>")
    inputMap.toList.foreach(tpl => sb.append("<tr><td align=\"center\">").append(tpl._1).append("</td><td>")
          .append(tpl._2).append("</td></tr>"))
    sb.append("</table></body></html>")
    sb.toString
  }
}