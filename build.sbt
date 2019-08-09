lazy val akkaHttpVersion = "10.1.9"
lazy val akkaVersion    = "2.6.0-M5"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "uk.gov.nationalarchives",
      scalaVersion    := "2.12.8"
    )),
    name := "TDR GraphQL akka-http API",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,

      "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"            % "3.0.5"         % Test,

      "org.sangria-graphql" %% "sangria" % "1.4.2",
      "org.sangria-graphql" %% "sangria-circe" % "1.2.1",

      // TODO: Is there an equivalent in the SDK v2?
      "com.amazonaws" % "aws-lambda-java-core" % "1.1.0"
    )
  )
