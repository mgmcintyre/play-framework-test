package controllers

import play.api.mvc.{Controller, Action, Result}
import play.api.Routes
import play.api.libs.ws.WS
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future

object Application extends Controller {

  def index = Action {
    val firstChoiceUrl  = "https://about.me/mgmcintyre"
    val secondChoiceUrl = "http://linkedin.com/in/mgmcintyre"
    Async {
      val start = System.currentTimeMillis()
      def getLatency(r: Any): Long = System.currentTimeMillis() - start
      
      val firstChoiceTime = WS.url(firstChoiceUrl).get().map(getLatency)
      val secondChoiceTime = WS.url(secondChoiceUrl).get().map(getLatency)
      
      Future.sequence(Seq(firstChoiceTime, secondChoiceTime)).map { case times =>
        play.api.Logger.debug("First: " + times(0) + ", Second: " + times(1))
        val url = if (times(0) > times(1)) 
          secondChoiceUrl
        else 
          firstChoiceUrl
        play.api.Logger.debug("Choice: " + url)
        TemporaryRedirect(url)
      } recover {
        case e =>
          play.api.Logger.warn("Issue fetching url: " + e.getMessage())
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