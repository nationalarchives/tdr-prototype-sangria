package uk.gov.nationalarchives

import io.circe.Json
import sangria.ast.Document
import sangria.execution.Executor
import sangria.marshalling.circe._
import sangria.parser.QueryParser
import sangria.schema.{Field, ObjectType, Schema, StringType, fields}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object GraphQlServer {

  val QueryType = ObjectType("Query", fields[ConsignmentDao, Unit](
    Field("hello", StringType, resolve = _ ⇒ "Hello world!"),
    // TODO: Return consignments
    Field("getConsignments", StringType, resolve = ctx => ctx.ctx.consignments)
  ))

  val schema = Schema(QueryType)

  // TODO: Should return Json, object or String?
  def send(request: GraphQlRequest): Future[Json] = {
    println(s"Got GraphQL request '$request'")

    val query: Try[Document] = QueryParser.parse(request.query)

    query match {
      case Success(doc) =>
        // TODO: Work out whether to pass JSON, String or object back to server
        Executor.execute(schema, doc, ConsignmentDao)
      case Failure(e) =>
        Future.failed(e)
    }
  }
}

case class GraphQlRequest(query: String)

trait ConsignmentDao {
  def consignments: String
}

object ConsignmentDao extends ConsignmentDao {
  override def consignments: String = "placeholder list of consignments"
}