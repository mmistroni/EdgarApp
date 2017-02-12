package edgar.util


object HtmlTableGenerator {
  
  def generateHtmlTable(inputMap:scala.collection.mutable.Map[String, Seq[String]]):String = {
    val sb = new StringBuilder("<html><body>")
    
    val securities = inputMap.values.flatten
    val accumulatedSecurities = securities.groupBy(item => item).toList.map(tpl => (tpl._1, tpl._2.size)).sortBy(_._2).reverse
    sb.append("<p>Top 30 Securities Held by Insitutional Investors</p><br/><br/>")
    
    sb.append("<table bgcolor=\"#B6EBF\" border=\"3\" cellspacing=\"4\">")
            .append("<th>Stock</th><th>#Investors</th>")
    
    accumulatedSecurities.slice(0,30).foreach(tpl => sb.append("<tr><td align=\"center\">").append(tpl._1).append("</td><td>")
          .append(tpl._2).append("</td></tr>"))
    sb.append("</table>")
    sb.append("<br/><br/><br/>")
    
    /**
    sb.append("<p> More Details </p><br/><br/>")
    
    
    sb.append("<table bgcolor=\"#B6EBF\" border=\"3\" cellspacing=\"4\">")
            .append("<th>InstitutionalManager</th><th>Securities Held</th>")
    inputMap.toList.foreach(tpl => sb.append("<tr><td align=\"center\">").append(tpl._1).append("</td><td>")
          .append(tpl._2).append("</td></tr>"))
    sb.append("</table>")
    * </body></html>")
    */
    sb.append("</body></html>")
    sb.toString
  }
  
  
  
}