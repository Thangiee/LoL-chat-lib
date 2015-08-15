name := "League-of-Legend-Chat-Lib-Scala"

version := "0.2"

scalaVersion := "2.11.7"

organization := "com.github.thangiee"

publishMavenStyle := true

resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "org.igniterealtime.smack" % "smack-java7" % "4.1.3",
  "org.igniterealtime.smack" % "smack-tcp" % "4.1.3",
  "org.igniterealtime.smack" % "smack-core" % "4.1.3",
  "org.igniterealtime.smack" % "smack-extensions" % "4.1.3",

  "org.scalactic" % "scalactic_2.11" % "2.2.5",

  "org.scalatest" % "scalatest_2.11" % "2.2.5" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test"
)

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

bintrayVcsUrl := Some("https://github.com/Thangiee/League-of-Legend-Chat-Lib-Scala")