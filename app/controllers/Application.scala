package controllers

import play.api._
import play.api.mvc.{ Controller, Action, Result }
import play.api.Routes
import play.api.libs.json._
import play.api.libs.ws.WS
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import java.net.{ InetAddress, DatagramPacket, DatagramSocket }

import play.api.cache.Cache
import play.api.Play.current
import com.typesafe.plugin.RedisPlugin

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Racing..."))
  }

  def redirectMe = Action {
    val firstChoice: List[String] = List(
      "https://about.me/mgmcintyre",
      "about-me")
    val secondChoice: List[String] = List(
      "http://linkedin.com/in/mgmcintyre",
      "linkedin")
    Async {
      val start = System.currentTimeMillis()
      def getLatency(r: Any): Long = System.currentTimeMillis() - start

      val firstTime = WS.url(firstChoice(0)).get().map(getLatency)
      val secondTime = WS.url(secondChoice(0)).get().map(getLatency)

      Future.sequence(Seq(firstTime, secondTime)).map { times =>
          play.api.Logger.debug("First: " + times(0) + ", Second: " + times(1))
          
          val choice = if (times(0) > times(1)) secondChoice
          else firstChoice

          play.api.Logger.debug("Choice: " + choice(0))
          val json = Json.obj(
            "status" -> "OK",
            "redirect" -> Json.obj (
              "winner" -> choice(0),
              "results" -> Json.arr (
                Json.obj (
                  "url" -> firstChoice(0),
                  "name" -> firstChoice(1),
                  "time" -> times(0)
                ),
                Json.obj (
                  "url" -> secondChoice(0),
                  "name" -> secondChoice(1),
                  "time" -> times(1)
                )
              )
            )
          )
          Ok(json)
      } recover {
        case e =>
          play.api.Logger.warn("Issue fetching url: " + e.getMessage)
          val json = Json.obj(
            "status" -> "KO",
            "errors" -> Json.arr(
              "Something went wrong!",
              e.getMessage
            )
          )
          InternalServerError(json)
      }
    }
  }

}