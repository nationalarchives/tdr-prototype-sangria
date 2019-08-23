package uk.gov.nationalarchives.tdr.api.core

import io.circe.Json
import sangria.ast.Document
import sangria.execution.Executor
import sangria.macros.derive._
import sangria.marshalling.circe._
import sangria.parser.QueryParser
import sangria.schema.{Argument, Field, IntType, ListType, ObjectType, OptionType, Schema, StringType, fields}
import uk.gov.nationalarchives.tdr.api.core.db.dao.{ConsignmentDao, FileDao, SeriesDao}
import uk.gov.nationalarchives.tdr.api.core.graphql.RequestContext
import uk.gov.nationalarchives.tdr.api.core.graphql.service.{ConsignmentService, FileService, SeriesService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object GraphQlServer {

  implicit private val SeriesType: ObjectType[Unit, Series] = deriveObjectType[Unit, Series]()
  implicit private val ConsignmentType: ObjectType[Unit, Consignment] = deriveObjectType[Unit, Consignment]()
  implicit private val FileType: ObjectType[Unit, File] = deriveObjectType[Unit, File]()

  private val ConsignmentNameArg = Argument("name", StringType)
  private val ConsignmentIdArg = Argument("id", IntType)
  private val SeriesIdArg = Argument("seriesId", IntType)
  private val filePathArg = Argument("path", StringType)
  private val FileIdArg = Argument("id", IntType)

  private val QueryType = ObjectType("Query", fields[RequestContext, Unit](
    Field("getConsignments", ListType(ConsignmentType), resolve = ctx => ctx.ctx.consignments.all),
    Field(
      "getConsignment",
      OptionType(ConsignmentType),
      arguments = List(ConsignmentIdArg),
      resolve = ctx => ctx.ctx.consignments.get(ctx.arg(ConsignmentIdArg))
    ),
    Field(
      "getFile",
      OptionType(FileType),
      arguments = List(FileIdArg),
      resolve = ctx => ctx.ctx.files.get(ctx.arg(FileIdArg))
    ),
    Field(
      "getFiles",
      ListType(FileType),
      resolve = ctx => ctx.ctx.files.all)
  ))

  private val MutationType = ObjectType("Mutation", fields[RequestContext, Unit](
    Field(
      "createConsignment",
      ConsignmentType,
      arguments = List(ConsignmentNameArg, SeriesIdArg),
      resolve = ctx => ctx.ctx.consignments.create(ctx.arg(ConsignmentNameArg), ctx.arg(SeriesIdArg))),
    Field(
      "createFile",
      FileType,
      arguments = List(filePathArg, ConsignmentIdArg),
      resolve = ctx => ctx.ctx.files.create(ctx.arg(filePathArg), ctx.arg(ConsignmentIdArg))
    )
  ))

  private val schema = Schema(QueryType, Some(MutationType))

  private val seriesService = new SeriesService(new SeriesDao)
  private val consignmentService = new ConsignmentService(new ConsignmentDao, seriesService)
  private val fileService = new FileService(new FileDao, consignmentService)
  private val requestContext = new RequestContext(seriesService, consignmentService, fileService)

  def send(request: GraphQlRequest): Future[Json] = {
    println(s"Got GraphQL request '$request'")

    val query: Try[Document] = QueryParser.parse(request.query)

    query match {
      case Success(doc) =>
        val variables = request.variables.getOrElse(Json.obj())
        Executor.execute(schema, doc, requestContext, operationName = request.operationName, variables = variables)
      case Failure(e) =>
        Future.failed(e)
    }
  }
}

case class GraphQlRequest(query: String, operationName: Option[String], variables: Option[Json])

case class Series(id: Int, name: String, description: String)
case class Consignment(id: Int, name: String, series: Series)
case class File(id: Int, path: String, consignment: Consignment)
