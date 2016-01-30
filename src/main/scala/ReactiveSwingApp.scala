
import scala.swing._
import scala.swing.event._
import rx.lang.scala._

object ReactiveSwingApp extends  SimpleSwingApplication {
  
  def top = new MainFrame {
    title = "Reactive Swing app"
    val button = new Button {
      text = "Click me"
    }
    
    val label = new Label {
      text = "No button clicks registered"
    }
    
    contents = new BoxPanel(Orientation.Vertical) {
      contents += button
      contents += label
      border = Swing.EmptyBorder(30,30, 10, 10)
    }
    
    val buttonObserver = 
                Observable.create[Button] { obs =>
                  button.reactions += {
                    case ButtonClicked(_) => obs.onNext(button)
                  }
                  Subscription()
              }
    
    
    //listenTo(button)
    var nClicks = 0
    
    val sub = buttonObserver.subscribe( _ => {
            nClicks += 1
            println("button licked")
            label.text = "Number of button clicks: " + nClicks
          })
      
    /**
          reactions += {
            case _  =>
              
          }
        })
    **/
    
  }    
 
  
}