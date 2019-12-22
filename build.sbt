enablePlugins(ScriptedPlugin)

organization := "com.supersonic"
name := "sonic-dependency-tree"
version := "0.0.1-SNAPSHOT"
sbtPlugin := true
scriptedLaunchOpts += ("-Dplugin.version=" + version.value)
scriptedBufferLog := false
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.8.0",
  "ai.x" %% "play-json-extensions" % "0.40.2",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.688"
)
