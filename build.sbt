
lazy val commonSettings = Seq(
  version := "0.3.4",
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq("-Xexperimental"),
  organization := "com.github.thangiee",
  publishMavenStyle := true,
  resolvers += Resolver.jcenterRepo,
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  bintrayVcsUrl := Some("https://github.com/Thangiee/LoL-chat-lib"),
  bintrayReleaseOnPublish in ThisBuild := false, // 1. publish 2. bintrayRelease
  libraryDependencies ++= {
    val catsVer = "0.6.1"
    Seq(
      "org.typelevel" %% "cats-macros" % catsVer,
      "org.typelevel" %% "cats-kernel" % catsVer,
      "org.typelevel" %% "cats-core" % catsVer,
      "org.typelevel" %% "cats-free" % catsVer
    )
  }
)

lazy val core = project
  .settings(commonSettings)
  .settings(
    name := "LoL-chat-core"
  )

lazy val lib = project
  .settings(commonSettings)
  .settings(
    name := "LoL-chat-lib",
    libraryDependencies ++= {
      val smackVer = "4.1.7"
      Seq(
        "org.igniterealtime.smack" % "smack-java7" % smackVer % "provided",
        "org.igniterealtime.smack" % "smack-tcp" % smackVer,
        "org.igniterealtime.smack" % "smack-core" % smackVer,
        "org.igniterealtime.smack" % "smack-extensions" % smackVer
      )
    },

    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "scalarx" % "0.3.1",
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
  .dependsOn(core)

