import Dependencies._

ThisBuild / scalaVersion     := "2.13.11"
ThisBuild / version          := "0.6-SNAPSHOT"
ThisBuild / organization     := "com.ideal.linked"

lazy val root = (project in file("."))
  .settings(
    name := "toposoid-feature-vectorizer",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "com.ideal.linked" %% "scala-common" % "0.6-SNAPSHOT",
    libraryDependencies += "com.ideal.linked" %% "toposoid-knowledgebase-model" % "0.6-SNAPSHOT",
    libraryDependencies += "com.ideal.linked" %% "toposoid-deduction-protocol-model" % "0.6-SNAPSHOT",
    libraryDependencies += "com.ideal.linked" %% "toposoid-common" % "0.6-SNAPSHOT",
    libraryDependencies += "io.jvm.uuid" %% "scala-uuid" % "0.3.1"
)
  .enablePlugins(AutomateHeaderPlugin)

organizationName := "Linked Ideal LLC.[https://linked-ideal.com/]"
startYear := Some(2021)
licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))
