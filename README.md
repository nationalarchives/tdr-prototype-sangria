# TDR prototype: GraphQL API

This project is part of the [Transfer Digital Records][tdr-docs] project. It is a prototype of the API that will be used
to get and store information about transferred files and the progress of the transfer.

It uses the Scala library [Sangria] to respond to GraphQL queries.

[tdr-docs]: https://github.com/nationalarchives/tdr-dev-documentation
[Sangria]: https://sangria-graphql.org/

## Design

The application is composed of three sbt projects: the core GraphQL logic and two entry points. One entry point is an
akka-http server, which can be run in development. The other is a request handler that can process an AWS Lambda event.

We are still trialling AWS Lambda for the API hosting. If cold starts turn out to be too slow, we may decide to deploy
the akka-http server to an ECS host instead.

## Development

To run the project in development, either run `sbt run` or run the `QuickstartServer` app from IntelliJ.

This will start a server at http://localhost:8080/. It currently just provides a GraphQL POST endpoint at
http://127.0.0.1:8080/graphql, which you can send queries to with curl, Postman or a GraphQL client.

## Deployment

* Create a new Lambda function with a Java 8 runtime
* Create a new API Gateway
* Add a POST endpoint at /graphql, and set the integration type to "Lambda Function". Do not check "Use Lambda Proxy
  integration", since this would send the whole HTTP request to the Lambda. The request handler is configured to parse
  just the POST body, not the whole request.
* Deploy the API Gateway
* Build the lambda sbt project locally by running `sbt clean lambda/assembly`, which should build a jar file at
  lambda/target/scala-2.12/tdr-api-lambda.jar
* Upload the jar file to the Lambda in the AWS console

You should then be able to POST GraphQL queries to your API Gateway URL (remembering to add `/graphql` to the end).
