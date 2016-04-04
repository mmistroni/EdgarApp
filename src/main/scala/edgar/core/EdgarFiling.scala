package edgar.core

case class EdgarFiling(cik: String, asOfDate: String,
                         formType: String, companyName: String, filingPath: String)
