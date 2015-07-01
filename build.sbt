name := "League-of-Legend-Chat-Lib-Scala"

version := "0.2"

scalaVersion := "2.11.7"

resolvers += "smack repo" at "https://oss.sonatype.org/content/repositories/snapshots"
resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
  "org.igniterealtime.smack" % "smack-java7" % "4.1.1",
  "org.igniterealtime.smack" % "smack-tcp" % "4.1.1",
  "org.igniterealtime.smack" % "smack-core" % "4.1.1",
  "org.igniterealtime.smack" % "smack-extensions" % "4.1.1",

  "org.scalactic" % "scalactic_2.11" % "2.2.5",

  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test"
)