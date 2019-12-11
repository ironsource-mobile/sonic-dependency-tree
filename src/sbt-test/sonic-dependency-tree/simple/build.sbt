
lazy val root = (project in file("."))
  .enablePlugins(SonicDependencyTreePlugin)
  .settings(
    scalaVersion := "2.11.12",
    version := "0.1"
  )
