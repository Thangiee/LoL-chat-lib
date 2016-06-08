name := "League-of-Legend-Chat-Lib-Scala"
version := "0.2.1"
scalaVersion := "2.11.8"
organization := "com.github.thangiee"
publishMavenStyle := true
resolvers += Resolver.jcenterRepo

val smackVer = "4.1.7"
libraryDependencies ++= Seq(
  "org.igniterealtime.smack" % "smack-java7" % smackVer % "provided",
  "org.igniterealtime.smack" % "smack-tcp" % smackVer,
  "org.igniterealtime.smack" % "smack-core" % smackVer,
  "org.igniterealtime.smack" % "smack-extensions" % smackVer,
)

libraryDependencies ++= Seq(
  "org.scalactic" % "scalactic_2.11" % "2.2.5"
)

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.11" % "2.2.5" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test"
)

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

bintrayVcsUrl := Some("https://github.com/Thangiee/League-of-Legend-Chat-Lib-Scala")