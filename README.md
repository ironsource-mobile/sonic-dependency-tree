# sonic-dependency-tree

[![Build Status](https://travis-ci.org/SupersonicAds/sonic-dependency-tree.svg?branch=master)](https://travis-ci.org/SupersonicAds/sonic-dependency-tree)

SBT plugin for retrieving Scala modules and libraries dependencies and outputs it as JSON. As an identifier for current dependencies, we use Git commit.
In addition, there's an option to upload the current commit project's dependencies to Amazon S3.

## Motivation
In our project, which is mono-repo, we have a lot of services and libraries, and it became hard to follow the module/library dependency graph. There are SBT plugins that can answer this question, but they usually contain a large number of settings and tasks that are not relevant to our needs.
We wanted to have a lightweight sbt command to understand our project's module dependencies.
With the help of Github API commit diff, we can use this information in order to determine which services were affected by the code change in a commit and deploy only these services.
It helps to keep track of module dependencies and library version used in each module.

## Installation
Add the plugin as SBT dependency to your `project/plugins.sbt`

```addSbtPlugin("com.supersonic" % "sonic-dependency-tree" % x)```

On your root project enable plugin
``` scala
lazy val root = (project in file("."))
  .enablePlugins(SonicDependencyTreePlugin)
```

## Settings

- `sonicDependenciesExcludeScalaLibrary` : `Boolean`
    - Setting to filter scala lang libraries from dependencies list
    - default: `false`
- `sonicDependenciesS3BasePath` : `String`
    - The base path when uploading to S3, the bucket name will be appended to it
    - default: ""
- `sonicDependenciesS3Bucket` : `String` 
    - The S3 bucket to upload the dependency tree
    - default: ""
- `sonicDependenciesS3Credentials` : Seq[Credentials]
    - The S3 credentials to use to upload to S3 with, if empty defaults taken from the environment or IAM will be used
    - default: Empty (see below)
- `sonicDependenciesUploadFilename` : `String` 
    - Name of the file that will be uploaded to S3, default is value of setting \"{gitCommitHash}.json\"
    - default: %gitCommitHash%.json (example: 025c34d07cf8558d25f477fad10fdf9122924e0f.json)

## Tasks
- `sonicDependenciesGitCommit` : `Task` 
    - Prints the current git HEAD SHA-1 commit hash
- `sonicDependencies` : `Task` 
    - Prints dependency list as JSON Array
- `sonicDependenciesWithCommit` : `Task` 
    - Prints dependency list with commit as JSON Array
- `sonicDependenciesUploadToS3` : `Task` 
    - Upload full dependency tree to S3


## S3 configuration
You can upload the dependency tree of the project to AWS S3 using this command `sbt sonicDependenciesUploadToS3`

Since the upload task is for the whole project, settings for it should be set on the root project.
For example:
``` scala
lazy val root = (project in file("."))
  .enablePlugins(SonicDependencyTreePlugin).
  .settings(
    sonicDependenciesS3BasePath := "my-folder",
    sonicDependenciesS3Bucket := "dependencies-bucket",
    sonicDependenciesUploadFilename := "dependencies.json"
  )
```
This will upload the file to `S3://dependencies-bucket/my-folder/dependencies.json`

## S3 credentials config

- WARNING: The preferred method, is to use `sonicDependenciesS3Credentials` as default and use this setting with hardcoded credentials for testing purposes ONLY!

AWS S3 client library used is `com.amazonaws.aws-java-sdk-s3`
In case `sonicDependenciesS3Credentials` is not set, S3 client will use  `DefaultAWSCredentialsProviderChain`, which is the preferred method and do not require to store credentials in `build.sbt` project definition, also it is more secure.
