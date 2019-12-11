package com.supersonic

import sbt._
import scala.sys.process.Process
import scala.util.{Failure, Success, Try}

object SonicDependencyTreePlugin extends AutoPlugin {
  object autoImport {
    val greeting = settingKey[String]("greeting")
    val hello = taskKey[Unit]("say hello")
    val dependenciesGitCommitHash = settingKey[String]("The current git HEAD SHA-1 commit hash")
    val dependenciesGitCommitHashPrint = taskKey[Unit]("Prints the current git HEAD SHA-1 commit hash")
  }

  import autoImport._

  override def trigger = allRequirements

  override lazy val buildSettings = Seq(
    greeting := "Hi!",
    hello := helloTask.value,
    dependenciesGitCommitHash := commitHash,
    dependenciesGitCommitHashPrint := println(dependenciesGitCommitHash.value))

  lazy val helloTask =
    Def.task {
      println(greeting.value)
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

}
