enablePlugins(ScriptedPlugin)
enablePlugins(BuildInfoPlugin)

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

sonatypeCredentialHost := Sonatype.sonatype01

buildInfoKeys := List[BuildInfoKey](
  name,
  version,
  scalaVersion,
  sbtVersion,
  "gitCommit" -> git.gitHeadCommit.value.getOrElse(""),
  "gitDescribedVersion" -> git.gitDescribedVersion.value.getOrElse(""))

buildInfoPackage := organization.value

makePomConfiguration := makePomConfiguration.value.withConfigurations(Configurations.defaultMavenConfigurations)

resolvers += Resolver.jcenterRepo

sbtPlugin := true
scriptedLaunchOpts += ("-Dplugin.version=" + version.value)
scriptedBufferLog := false

inThisBuild(List(
  organization := "com.supersonic",
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/SupersonicAds/sonic-dependency-tree")),
  developers := List(Developer("SupersonicAds", "SupersonicAds", "SupersonicAds", url("https://github.com/SupersonicAds"))),
  scmInfo := Some(ScmInfo(url("https://github.com/SupersonicAds/sonic-dependency-tree"), "scm:git:git@github.com:SupersonicAds/sonic-dependency-tree.git")),

  githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v"))),
  githubWorkflowTargetTags ++= Seq("v*"),
  githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test", "scripted"))),
  githubWorkflowPublish := Seq(
    WorkflowStep.Sbt(
      List("ci-release"),
      env = Map(
        "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
        "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
        "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
        "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}")))))

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.8.0",
  "ai.x" %% "play-json-extensions" % "0.40.2",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.688"
)
