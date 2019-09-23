package uk.gov.nationalarchives.tdr.api.httpserver

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Json
import io.circe.generic.auto._
import uk.gov.nationalarchives.tdr.api.core.GraphQlRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

trait Routes extends FailFastCirceSupport {

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[Routes])

  def graphQlActor: ActorRef

  // Required by the `ask` (?) method below
  private implicit lazy val timeout: Timeout = Timeout(15.seconds) // usually we'd obtain the timeout from the system's configuration

  //Use CORS' default settings to get local development to work.
  //Settings can be overridden using conf file or in code: https://github.com/lomigmegard/akka-http-cors
  lazy val graphQlRoutes: Route = cors() {
      pathPrefix("") {
        post {
          entity(as[GraphQlRequest]) {
            graphQlRequest => {
              val graphQlResponse: Future[Json] = (graphQlActor ? graphQlRequest).mapTo[Json]
              complete(graphQlResponse)
            }
          }
        }
      }
  }
}
