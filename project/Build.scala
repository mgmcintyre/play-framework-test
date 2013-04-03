import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "todolist"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    "postgresql" % "postgresql" % "8.4-702.jdbc4",
    "org.reactivemongo" %% "reactivemongo" % "0.8",
    "org.reactivemongo" %% "play2-reactivemongo" % "0.8",
    "com.typesafe" %% "play-plugins-redis" % "2.1-1-RC2"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
    resolvers += "Sedis Repo" at "http://pk11-scratch.googlecode.com/svn/trunk"
  )

}
