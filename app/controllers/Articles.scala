package controllers

import models._
import org.joda.time._
import play.api._
import play.api.libs.iteratee._
import play.api.mvc._
import play.api.Play.current
import play.modules.reactivemongo._
import scala.concurrent.{ ExecutionContext, Future }
import reactivemongo.api._
import reactivemongo.api.gridfs._
import reactivemongo.bson._
import reactivemongo.bson.handlers.DefaultBSONHandlers.DefaultBSONDocumentWriter
import reactivemongo.bson.handlers.DefaultBSONHandlers.DefaultBSONReaderHandler
import java.io.ByteArrayOutputStream

object Articles extends Controller with MongoController {
  val db = ReactiveMongoPlugin.db
  val collection = db("articles")

  // list all articles and sort them
  def index = Action { implicit request =>
    implicit val reader = Article.ArticleBSONReader
    
    Async {  
      // Get sorting info
      val sortParam = getSortParam(request)
      val sort = sortParam match {
        case Some(param) =>
          getSort(param)
        case _ => None
      }
      val activeSort = sortParam.getOrElse("none")
      
      // Build the query
      val query = BSONDocument(
        "$orderby" -> sort,
        "$query" -> BSONDocument())
      val found = collection.find(query)
      
      // Build (asynchronously) a list containing all the articles
      found.toList.map { articles =>
        Ok(views.html.articles(articles, activeSort)).withSession {
          "sort" -> activeSort
        }
      }
    }
  }

  def showCreationForm = Action {
    Ok(views.html.editArticle(None, Article.form))
  }

  def showEditForm(id: String) = Action {
    implicit val reader = Article.ArticleBSONReader

    Async {
      // Find specific document
      val objectId = new BSONObjectID(id)
      val cursor = collection.find(BSONDocument("_id" -> objectId))
      
      // Article edit page or NotFound redirect
      cursor.headOption.map { article =>
        article match {
          case article: Some[Article] => Ok(views.html.editArticle(Some(id), Article.form.fill(article.get)))
          case _ => NotFound
        }
      }
    }
  }

  def create = Action { implicit request =>
    Article.form.bindFromRequest.fold(
      errors => Ok(views.html.editArticle(None, errors)),
      // if no error, then insert the article into the 'articles' collection
      article => AsyncResult {
        val document = article.copy(creationDate = Some(new DateTime()),
            updateDate = Some(new DateTime()))
        collection.insert(document).map(_ =>
          Redirect(routes.Articles.index).flashing("message" -> "Article created!"))
      })
  }

  def edit(id: String) = Action { implicit request =>
    Article.form.bindFromRequest.fold(
      errors => Ok(views.html.editArticle(Some(id), errors)),
      article => AsyncResult {
        val objectId = new BSONObjectID(id)
        // create a modifier document, ie a document that contains the update operations to run onto the documents matching the query
        val modifier = BSONDocument(
          // this modifier will set the fields 'updateDate', 'title', 'content', and 'publisher'
          "$set" -> BSONDocument(
            "updateDate" -> BSONDateTime(new DateTime().getMillis),
            "title" -> BSONString(article.title),
            "content" -> BSONString(article.content),
            "publisher" -> BSONString(article.publisher)))
        // ok, let's do the update
        collection.update(BSONDocument("_id" -> objectId), modifier).map { _ =>
          Redirect(routes.Articles.index)
        }
      })
  }

  def delete(id: String) = Action {
    Async {
      collection.remove(BSONDocument("_id" -> new BSONObjectID(id))).map {
        _ => Redirect(routes.Articles.index)
      }.recover { case _ => InternalServerError }
    }
  }
  
  private def getSort(param: String): Option[BSONDocument] = {
    val direction: Int = if (param.startsWith("-")) -1 else 1
    val unsigned: String = if (direction == 1) param else param.drop(1)
    lazy val sort = BSONDocument(
      unsigned -> BSONInteger(direction)
    )
    if (! List("title", "publisher", "creationDate", "updateDate").contains(unsigned))
      None
    else Some(sort)
  }
  
  private def getSortParam(request: Request[_]): Option[String] = {
    request.queryString.get("sort") match {
      case Some(buffer) => Some(buffer.head)
      case _ => request.session.get("sort") match {
        case Some(string) => Some(string)
        case _ => None
      }
    }
  }

}