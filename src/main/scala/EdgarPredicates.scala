

package edgar.predicates {
  import edgar.core.EdgarFiling
  
  object EdgarPredicates {

    type EdgarFilter = EdgarFiling => Boolean
    
    def complement[A](predicate: A => Boolean) = (a: A) => !predicate(a)
    
    val cikEquals: String => EdgarFilter = cik => filing => filing.cik == cik

    val formTypeEquals: String => EdgarFilter = formType => filing => filing.formType == formType

    val companyNameEquals: String => EdgarFilter = companyName => filing => filing.companyName == companyName

    def formTypeIn: Set[String] => EdgarFilter = formTypes => filing => formTypes.contains(filing.formType)
    
    def formType2In: ((String)*) => EdgarFilter = formTypes => filing => formTypes.contains(filing.formType)
    
    def cikIn:Set[String] => EdgarFilter = ciks => filing => ciks.contains(filing.cik)

    def cikIn2:((String)*) =>EdgarFilter = ciks => filing => ciks.contains(filing.cik)
    
    def formTypesIn2:((String)*) =>EdgarFilter = formTypes => filing => formTypes.contains(filing.formType)
    
    def combinePredicates:(EdgarFilter,EdgarFilter) => EdgarFilter = (p, q) => filing => p(filing) && q(filing)
    
    def excludeFormTypes =  formType2In.andThen{ g => complement(g) } 

    def and(predicates: Seq[EdgarFilter])(filing: EdgarFiling) = predicates.forall(predicate => predicate(filing))

    def or(predicates: Seq[EdgarFilter])(filing: EdgarFiling) = predicates.exists(predicate => predicate(filing))

    def or2(predicates: (EdgarFilter)*)(filing: EdgarFiling) = predicates.exists(predicate => predicate(filing))
    
    
  }

}