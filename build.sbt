enablePlugins(ScriptedPlugin)
enablePlugins(BuildInfoPlugin)

organization := "com.supersonic"
name := "sonic-dependency-tree"
scalacOptions ++= List(
  "-encoding", "UTF-8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:higherKinds",
  "-Xfatal-warnings",
  "-Ywarn-value-discard",
  "-Xfuture",
  "-Xlint",
  "-Ypartial-unification")

buildInfoKeys := List[BuildInfoKey](
  name,
  version,
  scalaVersion,
  sbtVersion,
  "gitCommit" -> git.gitHeadCommit.value.getOrElse(""),
  "gitDescribedVersion" -> git.gitDescribedVersion.value.getOrElse(""))

buildInfoPackage := organization.value

resolvers += Resolver.jcenterRepo

sbtPlugin := true
scriptedLaunchOpts += ("-Dplugin.version=" + version.value)
scriptedBufferLog := false

inThisBuild(List(
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/SupersonicAds/sonic-dependency-tree")),
  developers := List(Developer("SupersonicAds", "SupersonicAds", "SupersonicAds", url("https://github.com/SupersonicAds"))),
  scmInfo := Some(ScmInfo(url("https://github.com/SupersonicAds/sonic-dependency-tree"), "scm:git:git@github.com:SupersonicAds/sonic-dependency-tree.git")),

  pgpPublicRing := file("./travis/local.pubring.asc"),
  pgpSecretRing := file("./travis/local.secring.asc"),
  releaseEarlyEnableSyncToMaven := false,
  releaseEarlyWith := BintrayPublisher))

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.8.0",
  "ai.x" %% "play-json-extensions" % "0.40.2",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.688"
)
