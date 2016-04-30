

package edgar.email

/**
import courier.Mailer
import courier.Text
import courier.addr

object MailerApp extends App{

  import courier._, Defaults._
  val mailer = Mailer("smtp.gmail.com", 587)
    .auth(true)
    .as("mmistroni@gmail.com", "80t5w4n4;")
    .startTtls(true)()

  println("Sending.....")
  val sender = mailer(Envelope.from("noreply" `@` "gmail.com")
    .to("mmistroni" `@` "gmail.com")
    .cc("simoneeadler" `@` "hotmail.com")
    .subject("miss you")
    .content(Text("h")))
    sender.onSuccess {
        case _ => println("message delivered")
    }
    sender.onFailure {
      case ex => println("Message failed!!")
    }

    println("Out of here")
    Thread.sleep(5000)
}
**/