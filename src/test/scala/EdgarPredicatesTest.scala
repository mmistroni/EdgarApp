import edgar.predicates.EdgarPredicates._
import edgar.core.EdgarFiling
import org.scalatest._
import Matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.junit._
import scala.io._
import Assert._

import org.mockito.{ Mockito, Matchers=>MockitoMatchers}


@RunWith(classOf[JUnitRunner])
class EdgarPredicatesTestSuite extends FunSuite  with Matchers { 
  
  def createEdgarFiling(cik: String, asOfDate:String , formType: String, companyName: String, 
                            filingPath: String):EdgarFiling = {
    EdgarFiling(cik, asOfDate, formType, companyName, filingPath)
  }
  
  test("cikEquals predicate ") {
    val (cik, asOfDate, formType, companyName, filingPath) = ("1", "20111021", "4", "xxx", "aaa")
  
    val testEdgarFiling = createEdgarFiling(cik, asOfDate, formType, companyName, filingPath)
    assertTrue(cikEquals(cik)(testEdgarFiling))
    assertFalse(cikEquals("foo")(testEdgarFiling))
  }
  
  test("companyName Equals predicate ") {
    val (cik, asOfDate, formType, companyName, filingPath) = ("1", "20111021", "4", "xxx", "aaa")
  
    val testEdgarFiling = createEdgarFiling(cik, asOfDate, formType, companyName, filingPath)
    assertTrue(companyNameEquals(companyName)(testEdgarFiling))
    assertFalse(companyNameEquals("anotherCompany")(testEdgarFiling))
  }
  
  test("formType Equals predicate ") {
    val (cik, asOfDate, formType, companyName, filingPath) = ("1", "20111021", "4", "xxx", "aaa")
  
    val testEdgarFiling = createEdgarFiling(cik, asOfDate, formType, companyName, filingPath)
    assertTrue(formTypeEquals(formType)(testEdgarFiling))
    assertFalse(formTypeEquals("5")(testEdgarFiling))
  }
  
  test("cikIn predicate ") {
    val (cik, asOfDate, formType, companyName, filingPath) = ("1", "20111021", "4", "xxx", "aaa")
  
    val testEdgarFiling = createEdgarFiling(cik, asOfDate, formType, companyName, filingPath)
    assertTrue(cikIn(Set(cik, "foo"))(testEdgarFiling))
    assertFalse(cikIn(Set("baz", "foo"))(testEdgarFiling))
  }
  
  test("formTypeIn predicate ") {
    val (cik, asOfDate, formType, companyName, filingPath) = ("1", "20111021", "4", "xxx", "aaa")
  
    val testEdgarFiling = createEdgarFiling(cik, asOfDate, formType, companyName, filingPath)
    assertTrue(formTypeIn(Set(cik, formType))(testEdgarFiling))
    assertFalse(cikIn(Set("baz", "foo"))(testEdgarFiling))
  }
  
  test("and predicate ") {
    val (cik, asOfDate, formType, companyName, filingPath) = ("1", "20111021", "4", "xxx", "aaa")
  
    val predicate1 = cikEquals(cik)
    val predicate2 = formTypeEquals("4")
    val combinedPredicates = List(predicate1, predicate2)
    
    val testEdgarFiling = createEdgarFiling(cik, asOfDate, formType, companyName, filingPath)
    val testEdgarFiling2 = testEdgarFiling.copy(cik="x")
    val testEdgarFiling3 = testEdgarFiling.copy(cik="xxxx", formType="4")
    assertTrue(and(combinedPredicates)(testEdgarFiling))
    assertFalse(and(combinedPredicates)(testEdgarFiling2))
    assertFalse(and(combinedPredicates)(testEdgarFiling3))
  }
  
  test("or predicate ") {
    val (cik, asOfDate, formType, companyName, filingPath) = ("1", "20111021", "4", "xxx", "aaa")
  
    val predicate1 = cikEquals(cik)
    val predicate2 = formTypeEquals(formType)
    val combinedPredicates = List(predicate1, predicate2)
    
    val testEdgarFiling = createEdgarFiling(cik, asOfDate, formType, companyName, filingPath)
    val testEdgarFiling2 = testEdgarFiling.copy(cik="x")
    val testEdgarFiling3 = testEdgarFiling.copy(cik="xxxx", formType="5")
    assertTrue(or(combinedPredicates)(testEdgarFiling))
    assertTrue(or(combinedPredicates)(testEdgarFiling2))
    assertFalse(or(combinedPredicates)(testEdgarFiling3))
  }
  
}