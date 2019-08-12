package uk.gov.nationalarchives

import io.circe.Json
import sangria.ast.Document
import sangria.execution.Executor
import sangria.macros.derive._
import sangria.marshalling.circe._
import sangria.parser.QueryParser
import sangria.schema.{Argument, Field, ListType, ObjectType, Schema, StringType, fields}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object GraphQlServer {

  private val ConsignmentType = deriveObjectType[ConsignmentDao, Consignment]()
  private val ConsignmentNameArg = Argument("name", StringType)

  private val QueryType = ObjectType("Query", fields[ConsignmentDao, Unit](
    Field("getConsignments", ListType(ConsignmentType), resolve = ctx => ctx.ctx.consignments)
  ))

  private val MutationType = ObjectType("Mutation", fields[ConsignmentDao, Unit](
    Field(
      "createConsignment",
      ConsignmentType,
      arguments = List(ConsignmentNameArg),
      resolve = ctx => ctx.ctx.create(Consignment(ctx.arg(ConsignmentNameArg))))
  ))

  private val schema = Schema(QueryType, Some(MutationType))

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
  def consignments: Seq[Consignment]
  def create(consignment: Consignment): Consignment
}

object ConsignmentDao extends ConsignmentDao {
  override def consignments: Seq[Consignment] = List(Consignment("Placeholder name"), Consignment("Other name"))
  // TODO: Store consignment
  override def create(consignment: Consignment): Consignment = consignment
}

case class Consignment(name: String)