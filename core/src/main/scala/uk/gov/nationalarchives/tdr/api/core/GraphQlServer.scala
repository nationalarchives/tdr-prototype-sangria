package uk.gov.nationalarchives.tdr.api.core

import io.circe.Json
import sangria.ast.Document
import sangria.execution.Executor
import sangria.macros.derive._
import sangria.marshalling.circe._
import sangria.parser.QueryParser
import sangria.schema.{Argument, Field, IntType, ListType, ObjectType, OptionType, Schema, StringType, fields}
import uk.gov.nationalarchives.tdr.api.core.db.dao.{ConsignmentDao, SeriesDao}
import uk.gov.nationalarchives.tdr.api.core.graphql.RequestContext
import uk.gov.nationalarchives.tdr.api.core.graphql.service.{ConsignmentService, SeriesService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object GraphQlServer {

  implicit private val SeriesType: ObjectType[Unit, Series] = deriveObjectType[Unit, Series]()
  implicit private val ConsignmentType: ObjectType[Unit, Consignment] = deriveObjectType[Unit, Consignment]()

  private val ConsignmentNameArg = Argument("name", StringType)
  private val ConsignmentIdArg = Argument("id", IntType)
  private val SeriesIdArg = Argument("seriesId", IntType)

  private val QueryType = ObjectType("Query", fields[RequestContext, Unit](
    Field("getConsignments", ListType(ConsignmentType), resolve = ctx => ctx.ctx.consignments.all),
    Field(
      "getConsignment",
      OptionType(ConsignmentType),
      arguments = List(ConsignmentIdArg),
      resolve = ctx => ctx.ctx.consignments.get(ctx.arg(ConsignmentIdArg))
    )
  ))

  private val MutationType = ObjectType("Mutation", fields[RequestContext, Unit](
    Field(
      "createConsignment",
      ConsignmentType,
      arguments = List(ConsignmentNameArg, SeriesIdArg),
      resolve = ctx => ctx.ctx.consignments.create(ctx.arg(ConsignmentNameArg), ctx.arg(SeriesIdArg)))
  ))

  private val schema = Schema(QueryType, Some(MutationType))

  private val seriesService = new SeriesService(new SeriesDao)
  private val consignmentService = new ConsignmentService(new ConsignmentDao, seriesService)
  private val requestContext = new RequestContext(seriesService, consignmentService)

  def send(request: GraphQlRequest): Future[Json] = {
    println(s"Got GraphQL request '$request'")

    val query: Try[Document] = QueryParser.parse(request.query)

    query match {
      case Success(doc) =>
        Executor.execute(schema, doc, requestContext)
      case Failure(e) =>
        Future.failed(e)
    }
  }
}

case class GraphQlRequest(query: String)

case class Series(id: Int, name: String, description: String)
case class Consignment(id: Int, name: String, series: Series)
