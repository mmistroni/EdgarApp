

package edgar.predicates {
  import edgar.core.EdgarFiling
  
  object EdgarPredicates {

    type EdgarFilter = EdgarFiling => Boolean

    val cikEquals: String => EdgarFilter = cik => filing => filing.cik == cik

    val formTypeEquals: String => EdgarFilter = formType => filing => filing.formType == formType

    val companyNameEquals: String => EdgarFilter = companyName => filing => filing.companyName == companyName

    def formTypeIn: Set[String] => EdgarFilter = formTypes => filing => formTypes.contains(filing.formType)

    def cikIn: Set[String] => EdgarFilter = cikList => filing => cikList.contains(filing.cik)

    def and(predicates: Seq[EdgarFilter])(filing: EdgarFiling) = predicates.forall(predicate => predicate(filing))

    def or(predicates: Seq[EdgarFilter])(filing: EdgarFiling) = predicates.exists(predicate => predicate(filing))

  }

}