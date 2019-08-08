package uk.gov.nationalarchives

import akka.actor.{ Actor, ActorLogging, Props }
import akka.pattern.pipe
import io.circe.Json
import sangria.execution._
import sangria.macros._
import sangria.marshalling.circe._
import sangria.schema._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object GraphQlActor {
  def props: Props = Props[GraphQlActor]
}

class GraphQlActor extends Actor with ActorLogging {

  val QueryType = ObjectType("Query", fields[Unit, Unit](
    Field("hello", StringType, resolve = _ ⇒ "Hello world!")))

  val schema = Schema(QueryType)

  val query = graphql"{ hello }"

  override def receive: Receive = {
    case _ =>
      val result: Future[Json] = Executor.execute(schema, query)
      // TODO: Work out whether to pass JSON, String or object back to server

      result.map(json => json.toString) pipeTo sender()
  }
}
