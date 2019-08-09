package uk.gov.nationalarchives

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

trait Routes extends JsonSupport {

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[Routes])

  def graphQlActor: ActorRef

  // Required by the `ask` (?) method below
  private implicit lazy val timeout: Timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  lazy val userRoutes: Route =
    pathPrefix("graphql") {
      post {
        entity(as[GraphQlRequest]) {
          graphQlQuery =>
            {
              val graphQlResponse: Future[Try[String]] = (graphQlActor ? graphQlQuery).mapTo[Try[String]]
              complete(graphQlResponse)
            }
        }
      }
    }
}

case class GraphQlRequest(query: String)

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val graphQlRequestFormat = jsonFormat1(GraphQlRequest)
}
