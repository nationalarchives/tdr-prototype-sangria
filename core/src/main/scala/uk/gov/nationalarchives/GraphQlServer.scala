package uk.gov.nationalarchives

import io.circe.Json
import sangria.ast.Document
import sangria.execution.Executor
import sangria.macros.derive._
import sangria.marshalling.circe._
import sangria.parser.QueryParser
import sangria.schema.{Argument, Field, IntType, ListType, ObjectType, OptionType, Schema, StringType, fields}
import uk.gov.nationalarchives.db.ConsignmentDao
import uk.gov.nationalarchives.model.Consignment

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object GraphQlServer {

  private val ConsignmentType = deriveObjectType[ConsignmentDao, Consignment]()
  private val ConsignmentNameArg = Argument("name", StringType)
  private val ConsignmentIdArg = Argument("id", IntType)

  private val QueryType = ObjectType("Query", fields[ConsignmentDao, Unit](
    Field("getConsignments", ListType(ConsignmentType), resolve = ctx => ctx.ctx.all),
    Field(
      "getConsignment",
      OptionType(ConsignmentType),
      arguments = List(ConsignmentIdArg),
      resolve = ctx => ctx.ctx.get(ctx.arg(ConsignmentIdArg))
    )
  ))

  private val MutationType = ObjectType("Mutation", fields[ConsignmentDao, Unit](
    Field(
      "createConsignment",
      ConsignmentType,
      arguments = List(ConsignmentNameArg),
      resolve = ctx => ctx.ctx.create(Consignment(name = ctx.arg(ConsignmentNameArg))))
  ))

  private val schema = Schema(QueryType, Some(MutationType))
  private val consignmentDao = new ConsignmentDao

  def send(request: GraphQlRequest): Future[Json] = {
    println(s"Got GraphQL request '$request'")

    val query: Try[Document] = QueryParser.parse(request.query)

    query match {
      case Success(doc) =>
        Executor.execute(schema, doc, consignmentDao)
      case Failure(e) =>
        Future.failed(e)
    }
  }
}

case class GraphQlRequest(query: String)
