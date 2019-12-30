sonicDependenciesExcludeScalaLibrary in Global:= true
sonicDependenciesS3BasePath in Global := "s3testPath"
sonicDependenciesS3Bucket in Global := "s3testBucket"
sonicDependenciesUploadFilename in Global := "testFileName.json"

lazy val root = (project in file("."))
  .settings(
    scalaVersion := "2.11.12",
    version := "0.1",
    TaskKey[Unit]("check") in Global := {
      if(sonicDependenciesExcludeScalaLibrary.value != true) sys.error("sonicDependenciesExcludeScalaLibrary not correct")
      if(sonicDependenciesS3BasePath.value != "s3testPath") sys.error("sonicDependenciesS3BasePath not correct")
      if(sonicDependenciesS3Bucket.value != "s3testBucket") sys.error("sonicDependenciesS3Bucket not correct")
      if(sonicDependenciesUploadFilename.value != "testFileName.json") sys.error("sonicDependenciesUploadFilename not correct")
      ()
    }
  )
