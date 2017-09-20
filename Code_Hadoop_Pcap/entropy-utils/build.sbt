import Dependencies._

libraryDependencies += "org.apache.spark" % "spark-core_2.10" % "2.2.0"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "org.tb.entropy",
      scalaVersion := "2.11.11",
      version      := "1.0.0"
    )),
    name := "entropy-utils",
    libraryDependencies += scalaTest % Test
  )
