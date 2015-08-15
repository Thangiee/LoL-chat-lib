name := "League-of-Legend-Chat-Lib-Scala"

version := "0.2"

scalaVersion := "2.11.7"

organization := "com.github.thangiee"

publishMavenStyle := true

resolvers ++= Seq(
  "sonatype repo" at "https://oss.sonatype.org/content/repositories/snapshots",
  "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  "thangiee-bintray" at "http://dl.bintray.com/thangiee/maven"
)

libraryDependencies ++=
  "org.igniterealtime.smack" % "smack-java7" % "4.1.1" ::
  "org.igniterealtime.smack" % "smack-tcp" % "4.1.1" ::
  "org.igniterealtime.smack" % "smack-core" % "4.1.1" ::
  "org.igniterealtime.smack" % "smack-extensions" % "4.1.1" ::
  "org.scalactic" % "scalactic_2.11" % "2.2.5" ::
  Nil

libraryDependencies ++=
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test" ::
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test" ::
  Nil

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

bintrayVcsUrl := Some("https://github.com/Thangiee/League-of-Legend-Chat-Lib-Scala")
