
package edgar.parsers

import scala.util.parsing.combinator._
import scala.util.parsing.combinator.syntactical._

/**
 *   Sample inputs
 *   (buy 100 IBM shares at max USD 45,
 *   sell 50 CISCO shares at min USD 25,
 *   buy 100 Google shares at max USD 800) for trading_account "SSS1234"
 */

object EdgarParserSample extends App {

  object OrderDSL extends StandardTokenParsers {
    lexical.delimiters ++= List("(", ")", ",")
    lexical.reserved += ("buy", "cikIn", "sell", "shares", "at", "max", "min", "for", "trading", "account")

    def instr = trans ~ account_spec

    def trans = "(" ~> repsep(trans_spec, ",") <~ ")"

    def rule = rule_name ~> "(" ~ stringLit ~> ")"

    def trans_spec = buy_sell ~ buy_sell_instr

    def account_spec = "for" ~> "trading" ~> "account" ~> stringLit

    def rule_name = "cikIn|formTypeIn"

    def buy_sell = ("buy" | "sell")

    def rule_combinators = "and|or"

    def buy_sell_instr = security_spec ~ price_spec

    def security_spec = numericLit ~ ident ~ "shares"

    def price_spec = "at" ~ ("min" | "max") ~ numericLit
  }

  object Simpletalk extends StandardTokenParsers with App {
    lexical.reserved += ("print", "space", "HELLO", "GOODBYE")
    import scala.io._
    val input = Source.fromFile("input.talk").getLines.reduceLeft[String](_ + '\n' + _)
    val tokens = new lexical.Scanner(input)

    val result = phrase(program)(tokens)

    // grammar starts here
    def program = stmt+

    def stmt = ("print" ~ greeting
      | "space")

    def greeting = ("HELLO"
      | "GOODBYE")
  }

}