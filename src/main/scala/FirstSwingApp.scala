

/**
 * @author marco
 */
import scala.swing._
import scala.swing.event._
import rx.lang.scala._

  object FirstSwingApp extends SimpleSwingApplication {
    def top = new MainFrame {
      title = "First Swing App"
      contents = new Button {
        text = "Click me"
      }
    }
  }

object SecondSwingApp extends SimpleSwingApplication {
  def top = new MainFrame {
    var nClicks = 0
    
    // subscription should be linked to reactionos
    
    //def textValues: Observable[String] = ???

       
    title = "Second Swing App"
    val button = new Button {
      text = "Click me"
    }
    val label = new Label {
      text = "No button clicks registered"
    } 
    
    val clicks = Observable.create[Button] {
        obs =>
          println("Observable...")
          obs.onNext(button)
          Subscription()
    }
    
    // Improve and understand why it can be done in one line
    clicks.subscribe(x => {
                        reactions += {
                            case ButtonClicked(b)   =>
                                  println("Clicking..")
                                  nClicks += 1
                                  label.text = "Number of button clicks: "+ nClicks
                        }
                      })
    
    
    contents = new BoxPanel(Orientation.Vertical) {
      contents += button
      contents += label
      border = Swing.EmptyBorder(30, 30, 10, 30)
    }
    
    listenTo(button)
    
    
  }
}  
