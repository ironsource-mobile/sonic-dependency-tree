package com.supersonic

import com.amazonaws.{ClientConfiguration, Protocol}
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials, DefaultAWSCredentialsProviderChain}
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import play.api.libs.json.{Json, Writes}
import sbt._
import scala.sys.process.Process
import scala.util.{Failure, Success, Try}

object SonicDependencyTreePlugin extends AutoPlugin {
  object autoImport {
    lazy val sonicDependenciesGitCommit = taskKey[Unit]("Prints the current git HEAD SHA-1 commit hash")

    lazy val sonicDependenciesExcludeScalaLibrary = settingKey[Boolean]("Setting to filter scala lang libraries from dependencies")
    lazy val sonicDependencies = taskKey[Unit]("Prints dependency graph as JSON Array")
    lazy val sonicDependenciesWithCommit = taskKey[Unit]("Prints dependency graph with commit as JSON Array")

    lazy val sonicDependenciesS3BasePath = settingKey[String]("The base path when uploading to S3, the bucket name will be appended to it")
    lazy val sonicDependenciesS3Bucket = settingKey[String]("The S3 bucket to upload the dependency tree")
    lazy val sonicDependenciesS3Credentials = settingKey[Seq[Credentials]]("The S3 credentials to use to upload to S3 with, if empty defaults taken from the environment or IAM will be used")
    lazy val sonicDependenciesUploadFilename = settingKey[String]("Name of the file that will be uploaded to S3, default is value of setting \"{gitCommitHash}.json\"")
    lazy val sonicDependenciesUploadToS3 = taskKey[Unit]("Upload full dependency tree to S3")
  }

  import autoImport._

  override def trigger = allRequirements

  override lazy val buildSettings = Seq(
    sonicDependenciesExcludeScalaLibrary in Global := false,
    sonicDependenciesGitCommit := println(commitHash),
    sonicDependenciesS3BasePath in Global := "",
    sonicDependenciesS3Bucket in Global := "",
    sonicDependenciesS3Credentials := Seq.empty,
    sonicDependenciesUploadFilename := s"$commitHash.json",
    sonicDependencies := printTreeTask.value,
    sonicDependenciesWithCommit := printTreeWithCommitTask.value,
    sonicDependenciesUploadToS3 := uploadDependenciesTask.value
  )

  private def uploadDependenciesTask = Def.task {

    val s3BasePathValue = sonicDependenciesS3BasePath.value
    val s3BucketValue = sonicDependenciesS3Bucket.value

    val log = Keys.streams.value.log

    if (Option(s3BucketValue).isEmpty) {
      throw new MessageOnlyException("sonicDependenciesS3Bucket is not set")
    }
    if (Option(s3BasePathValue).isEmpty) {
      throw new MessageOnlyException("sonicDependenciesS3BasePath is not set")
    }

    val uploadFilenameValue = sonicDependenciesUploadFilename.value

    val treeJson = asJson(dependencyTreeWithCommit.value)

    val client = getS3Client(sonicDependenciesS3Credentials.value, "")
    val bucketName = s"$s3BucketValue/$s3BasePathValue"
    log.info(s"Uploading dependency tree to: s3://$bucketName/$uploadFilenameValue")
    val _ = client.putObject(bucketName, uploadFilenameValue, treeJson)
  }

  private def printTreeWithCommitTask = Def.task {
    val graph = dependencyTreeWithCommit.value
    val modulesJson = asJson(graph)
    println(modulesJson)
  }

  private def printTreeTask = Def.task {
    val modules = dependencyTree.value
    val modulesJson = asJson(modules)
    println(modulesJson)
  }

  private val commitHash = {
    Try(Process("git rev-parse HEAD").!!.trim) match {
      case Failure(exception) =>
        val logger = ConsoleLogger()
        logger.warn("NOT a GIT repository. 'dependenciesGitCommitHash' setting will be set to empty string!")
        ""
      case Success(value) => value
    }
  }

  private def dependencyTreeWithCommit = Def.task {
    val modules = dependencyTree.value
    DependencyGraph(
      commit = commitHash,
      modules = modules.toList
    )
  }

  private val dependencyTree = {
    val buildDepMap = {
      Def.taskDyn {
        def evaluateTask[T](taskKey: ScopedKey[sbt.Task[T]], projectRef: ProjectRef) = {
          EvaluateTask.apply(Keys.buildStructure.value, taskKey, Keys.state.value, projectRef)
        }

        def evaluateSetting[T](settingKey: SettingKey[T], projectRef: ProjectRef) = {
          Project.extract(Keys.state.value).get(settingKey in ThisScope in projectRef)
        }

        def projectDependencies(projectRef: ProjectRef) = {
          evaluateTask(Keys.projectDependencies.in(ThisScope), projectRef) match {
            case Some((_, Value(moduleIds))) => moduleIds.filter(m => m.configurations.isEmpty).toList
            case _ => List.empty
          }
        }

        def projectLibraryDependencies(projectRef: ProjectRef) = {
          val libraries = evaluateSetting(Keys.libraryDependencies, projectRef)
          val excludeScalaLibrarySetting = evaluateSetting(sonicDependenciesExcludeScalaLibrary, projectRef)
          if (excludeScalaLibrarySetting)
            libraries.filterNot(isScalaLibraryId)
          else
            libraries
        }

        def transformModuleToString(moduleID: ModuleID) =
          s"${moduleID.organization}:${moduleID.name}:${moduleID.revision}"

        def isScalaLibraryId(moduleID: ModuleID) = moduleID.organization == "org.scala-lang" && moduleID.name == "scala-library"

        def projectSourcePath(projectRef: ProjectRef): File = {
          Project.extract(Keys.state.value).get(Keys.baseDirectory in Keys.configuration in projectRef)
        }

        def projectRelativePath(projectRef: ProjectRef) = {
          val rootPath = Keys.buildStructure.value.root.getPath
          val projectPath = projectSourcePath(projectRef)
          val rootFile = new java.io.File(rootPath)
          val relative = Path(projectPath).relativeTo(rootFile)
          relative.map(_.getPath).getOrElse("")
        }

        val refs = Keys.loadedBuild.value.allProjectRefs

        val dependenciesTask = refs.map(_._1).map {
          ref =>
            val modules = projectDependencies(ref)
            val projectPath = projectRelativePath(ref)
            val libraryDependencies = projectLibraryDependencies(ref)
            val filteredLibraryDependencies = libraryDependencies.map(transformModuleToString).toList

            ref -> Module(
              module = ref.project,
              dependencies = modules.map(_.name),
              libraryDependencies = filteredLibraryDependencies,
              sourcePath = projectPath)
        }

        Def.task {
          dependenciesTask
        }
      }
    }

    Def.task {
      val dependencyMap = buildDepMap.value.toMap
      dependencyMap.map { case (_, modules) => modules }
    }
  }

  private def getS3Client(credentials: Seq[Credentials], host: String) = {
    def s3ClientConfig(): ClientConfiguration = {
      def doWith(prop: String)(f: String => Unit) =
        sys.props.get(prop).foreach(f)

      val config = new ClientConfiguration().withProtocol(Protocol.HTTPS)

      doWith("http.proxyHost")(config.setProxyHost)
      doWith("http.proxyPort")(port => config.setProxyPort(port.toInt))
      doWith("http.proxyUser")(config.setProxyUsername)
      doWith("http.proxyPassword")(config.setProxyPassword)

      config
    }

    val usedCredentialsProvider = Credentials.forHost(credentials, host) match {
      case Some(credential) =>
        val credentials = new BasicAWSCredentials(credential.userName, credential.passwd)
        new AWSStaticCredentialsProvider(credentials)
      case None =>
        new DefaultAWSCredentialsProviderChain
    }

    AmazonS3ClientBuilder
      .standard()
      .withClientConfiguration(s3ClientConfig())
      .withCredentials(usedCredentialsProvider)
      .build()
  }

  private def asJson[T](obj: T)(implicit writes: Writes[T]) = Json.toJson(obj).toString()
}
