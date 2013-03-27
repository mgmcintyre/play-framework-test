package controllers

import play.api.mvc.{Controller, Action, Result}
import play.api.Routes
import play.api.libs.ws.WS
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import java.net.{InetAddress, DatagramPacket, DatagramSocket}

object Application extends Controller {

  def sendToGraphite(stat: String, value: Long): Boolean = {
    sendToGraphite(stat, value.toInt)
  }
  
  def sendToGraphite(stat: String, value: Int): Boolean = {
    val apikey = System.getenv("HOSTEDGRAPHITE_APIKEY") match {
      case null => "5a311dbe-b99e-40d7-b3b2-21208fa766ce"
      case s: String => s 
    }
    try {
      val sock = new DatagramSocket()
      val addr = InetAddress.getByName("alt.carbon.hostedgraphite.com")
      val message: Array[Byte] = (apikey + "." + stat + " " + value + "\n").getBytes()
      val packet = new DatagramPacket(message, message.length, addr, 2003)
      sock.send(packet)
      sock.close()
      true
    } catch {
      case e: Exception =>
        play.api.Logger.warn("Error connecting to Graphite: " + e.getMessage())
        false
    }
  }
  
  def index = Action {
    val firstChoice: List[String]  = List("https://about.me/mgmcintyre", "about-me")
    val secondChoice: List[String] = List("http://linkedin.com/in/mgmcintyre", "linkedin")
    Async {
      val start = System.currentTimeMillis()
      def getLatency(r: Any): Long = System.currentTimeMillis() - start
      
      val firstChoiceTime = WS.url(firstChoice(0)).get().map(getLatency)
      val secondChoiceTime = WS.url(secondChoice(0)).get().map(getLatency)
      
      Future.sequence(Seq(firstChoiceTime, secondChoiceTime)).map { case times =>
        play.api.Logger.debug("First: " + times(0) + ", Second: " + times(1))
        sendToGraphite("redirect.load." + firstChoice(1), times(0))
        sendToGraphite("redirect.load." + secondChoice(1), times(1))
        
        val choice: List[String] = if (times(0) > times(1)) 
          secondChoice
        else 
          firstChoice
        
        play.api.Logger.debug("Choice: " + choice(0))
        sendToGraphite("redirect.selected." + choice(1), 1)
        TemporaryRedirect(choice(0))
      } recover {
        case e =>
          play.api.Logger.warn("Issue fetching url: " + e.getMessage())
          sendToGraphite("redirect.error", 1)
          Ok("<h1>Something isn't quite right...</h1>" +
              "<p>My email address is <em>mark [at] mgmcintyre " +
              "[dot] co [dot] uk</em></p>").as(HTML)
      }
    }
  }
  
  def javascriptRoutes = Action { implicit request =>
    import routes.javascript._
    Ok(
      Routes.javascriptRouter("jsRoutes")(
        routes.javascript.Tasks.tasks
      )
    ).as("text/javascript")
  }
  
}