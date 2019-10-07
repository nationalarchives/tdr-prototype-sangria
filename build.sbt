ThisBuild / organization := "uk.gov.nationalarchives"
ThisBuild / scalaVersion := "2.12.9"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val akkaHttpVersion = "10.1.9"
lazy val akkaVersion    = "2.6.0-M5"
lazy val awsSdkVersion = "2.9.11"

enablePlugins(GraphQLSchemaPlugin)

graphqlSchemaSnippet := "uk.gov.nationalarchives.tdr.api.core.graphql.GraphQlTypes.schema"

val awsSdkConfigFiles = Set(
  "customization.config",
  "examples-1.json",
  "paginators-1.json",
  "service-2.json"
)

lazy val core = (project in file("core"))
  .settings(
    name := "TDR GraphQL API core",
    assemblyJarName in assembly := "tdr-api-core.jar",
    libraryDependencies ++= Seq(
      "org.sangria-graphql" %% "sangria" % "1.4.2",
      "org.sangria-graphql" %% "sangria-circe" % "1.2.1",
      "com.typesafe.slick" %% "slick" % "3.3.1",
      "org.slf4j" % "slf4j-nop" % "1.7.26",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.3.1",
      "org.postgresql" % "postgresql" % "42.2.6",
      "software.amazon.awssdk" % "ecs" % awsSdkVersion,
      "software.amazon.awssdk" % "ssm" % awsSdkVersion,
      "io.circe" %% "circe-generic" % "0.9.3",
    )
  )

lazy val root = (project in file("."))
  .settings(
    name := "TDR GraphQL akka-http API",
    assemblyJarName in assembly := "tdr-api.jar",
    mainClass in assembly := Some("uk.gov.nationalarchives.tdr.api.httpserver.ApiServer"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
      "ch.megard"         %% "akka-http-cors"       % "0.4.1",
      "de.heikoseeberger" %% "akka-http-circe"      % "1.27.0",
     
      "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"            % "3.0.5"         % Test,
    ),
 assemblyMergeStrategy in assembly := {
        case x if x.contains("io.netty.versions.properties") => MergeStrategy.discard
        case PathList(ps@_*) if awsSdkConfigFiles.contains(ps.last) =>
          MergeStrategy.discard
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
  }
  ).dependsOn(core)

mainClass in (Compile, run) := Some("uk.gov.nationalarchives.tdr.api.httpserver.ApiServer")
