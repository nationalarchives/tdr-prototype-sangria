package uk.gov.nationalarchives.tdr.api.httpserver

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import io.circe.Json
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import uk.gov.nationalarchives.tdr.api.core.GraphQlRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

trait Routes extends JsonSupport {

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[Routes])

  def graphQlActor: ActorRef

  // Required by the `ask` (?) method below
  private implicit lazy val timeout: Timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  //Use CORS' default settings to get local development to work.
  //Settings can be overridden using conf file or in code: https://github.com/lomigmegard/akka-http-cors
  lazy val graphQlRoutes: Route = cors() {
      pathPrefix("graphql") {
        post {
          entity(as[GraphQlRequest]) {
            graphQlQuery =>
            {
              val graphQlResponse: Future[Json] = (graphQlActor ? graphQlQuery).mapTo[Json]
              complete(graphQlResponse.map(_.toString))
            }
          }
        }
      }
  }
}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val graphQlRequestFormat: RootJsonFormat[GraphQlRequest] = jsonFormat1(GraphQlRequest)
}
