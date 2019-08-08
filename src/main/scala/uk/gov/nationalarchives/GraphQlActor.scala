package uk.gov.nationalarchives

import akka.actor.{ Actor, ActorLogging, Props }
import akka.pattern.pipe
import io.circe.Json
import sangria.ast.Document
import sangria.execution._
import sangria.marshalling.circe._
import sangria.parser.QueryParser
import sangria.schema._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }

object GraphQlActor {
  def props: Props = Props[GraphQlActor]
}

class GraphQlActor extends Actor with ActorLogging {

  val QueryType = ObjectType("Query", fields[Unit, Unit](
    Field("hello", StringType, resolve = _ ⇒ "Hello world!")))

  val schema = Schema(QueryType)

  override def receive: Receive = {
    case query: String =>
      println(s"Got query '$query'")
      val graphQlQuery: Try[Document] = QueryParser.parse(query)

      // TODO: Work out whether to pass JSON, String or object back to server
      graphQlQuery match {
        case Success(doc) =>
          val result: Future[Json] = Executor.execute(schema, doc)
          result.map(json => Success(json.toString)) pipeTo sender()
        case failure =>
          sender ! failure
      }
    case other =>
      println(s"Got other '$other'")
      sender() ! Failure(new RuntimeException(s"Could not parse message $other"))
  }
}
