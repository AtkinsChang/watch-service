import sbt._

trait Dependencies {
  private[this] val MockitoVersion = "2.9.0"

  lazy val Testing = Seq(
    "org.mockito" % "mockito-core" % MockitoVersion,
    "org.scalatest" %% "scalatest" % "3.0.4"
  )
}

object Dependencies extends Dependencies
