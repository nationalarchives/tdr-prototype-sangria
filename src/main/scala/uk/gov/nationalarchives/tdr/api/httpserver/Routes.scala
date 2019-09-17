package uk.gov.nationalarchives.tdr.api.httpserver

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.Logging
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Json
import io.circe.generic.auto._
import uk.gov.nationalarchives.tdr.api.core.GraphQlRequest

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}


trait Routes extends FailFastCirceSupport {

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[Routes])

    def graphQlActor: ActorRef


  // Required by the `ask` (?) method below
  private implicit lazy val timeout: Timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

//  implicit val as = ActorSystem("example")

  //Use CORS' default settings to get local development to work.
  //Settings can be overridden using conf file or in code: https://github.com/lomigmegard/akka-http-cors


  object Route {
    val as = ActorSystem("example")

    val websocketRoute = path("connect") {
      val handler = as.actorOf(Props[WsActor])
      val futureFlow = (handler ? GetWebsocketFlow).mapTo[Flow[Message, Message, _]]
      onComplete(futureFlow) {
        case Success(flow) => handleWebSocketMessages(flow)
        case Failure(err) => complete(err.toString)
      }
    }
  }




  lazy val graphQlRoutes: Route = cors() {
      path("") {
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
