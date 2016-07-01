
lazy val root = (project in file("."))
  .settings(
    name := "League-of-Legend-Chat-Lib-Scala",
    version := "0.3.0",
    scalaVersion := "2.11.8",
    scalacOptions ++= Seq("-Xexperimental"),
    organization := "com.github.thangiee",
    publishMavenStyle := true,
    resolvers += Resolver.jcenterRepo,
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    bintrayVcsUrl := Some("https://github.com/Thangiee/League-of-Legend-Chat-Lib-Scala"),
    bintrayReleaseOnPublish in ThisBuild := false, // 1. publish 2. bintrayRelease

    libraryDependencies ++= {
      val smackVer = "4.1.7"
      Seq(
        "org.igniterealtime.smack" % "smack-java7" % smackVer % "provided",
        "org.igniterealtime.smack" % "smack-tcp" % smackVer,
        "org.igniterealtime.smack" % "smack-core" % smackVer,
        "org.igniterealtime.smack" % "smack-extensions" % smackVer
      )
    },

    libraryDependencies ++= {
      val catsVer = "0.6.0"
      Seq(
        "org.typelevel" %% "cats-macros" % catsVer,
        "org.typelevel" %% "cats-kernel" % catsVer,
        "org.typelevel" %% "cats-core" % catsVer,
        "org.typelevel" %% "cats-free" % catsVer,
        "com.github.thangiee" %% "scala-frp" % "1.2"
      )
    },

    libraryDependencies ++= Seq(
      "org.scalatest" % "scalatest_2.11" % "2.2.5" % "it,test"
    )
  )
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(
    parallelExecution in IntegrationTest := false,
    scalaSource in IntegrationTest := baseDirectory.value / "src/it/scala",
    TaskKey[Unit]("test-all") <<= (test in IntegrationTest).dependsOn(test in Test)
  )

