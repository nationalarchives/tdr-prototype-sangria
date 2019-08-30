package uk.gov.nationalarchives.tdr.api.core

import io.circe.Json
import io.circe.generic.auto._
import sangria.ast.Document
import sangria.execution.Executor
import sangria.macros.derive._
import sangria.marshalling.circe._
import sangria.parser.QueryParser
import sangria.schema.{Argument, BooleanType, Field, IntType, ListInputType, ListType, ObjectType, OptionType, Schema, StringType, fields}
import uk.gov.nationalarchives.tdr.api.core.db.dao._
import uk.gov.nationalarchives.tdr.api.core.graphql.RequestContext
import uk.gov.nationalarchives.tdr.api.core.graphql.service._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object GraphQlServer {

  implicit private val SeriesType: ObjectType[Unit, Series] = deriveObjectType[Unit, Series]()
  implicit private val ConsignmentType: ObjectType[Unit, Consignment] = deriveObjectType[Unit, Consignment]()
  implicit private val FileType: ObjectType[Unit, File] = deriveObjectType[Unit, File]()
  implicit private val FileStatusType: ObjectType[Unit, FileStatus] = deriveObjectType[Unit, FileStatus]()
  implicit private val CreateFileInputType = deriveInputObjectType[CreateFileInput]()

  private val ConsignmentNameArg = Argument("name", StringType)
  private val ConsignmentIdArg = Argument("id", IntType)
  private val SeriesIdArg = Argument("seriesId", IntType)
  private val FileIdArg = Argument("id", IntType)
  private val ChecksumArg = Argument("checksum", StringType)
  private val VirusCheckStatusArg = Argument("status", StringType)
  private val PronomIdArg = Argument("pronomId", StringType)
  private val FileInputArg = Argument("createFileInput", CreateFileInputType)
  private val MultipleFileInputsArg = Argument("createFileInputs", ListInputType(CreateFileInputType))

  private val QueryType = ObjectType("Query", fields[RequestContext, Unit](
    Field(
      "getConsignments",
      ListType(ConsignmentType),
      resolve = ctx => ctx.ctx.consignments.all),
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
      arguments = List(FileInputArg),
      resolve = ctx => ctx.ctx.files.create(ctx.arg(FileInputArg))
    ),
    Field(
      "updateServerSideFileChecksum",
      BooleanType,
      arguments = List(FileIdArg, ChecksumArg),
      resolve = ctx => ctx.ctx.fileStatuses.updateServerSideChecksum(ctx.arg(FileIdArg), ctx.arg(ChecksumArg))
    ),
    Field(
      "updateClientSideFileChecksum",
      BooleanType,
      arguments = List(FileIdArg, ChecksumArg),
      resolve = ctx => ctx.ctx.fileStatuses.updateClientSideChecksum(ctx.arg(FileIdArg), ctx.arg(ChecksumArg))
    ),
      Field(
      "updateVirusCheck",
      BooleanType,
      arguments = List(FileIdArg, VirusCheckStatusArg),
      resolve = ctx => ctx.ctx.fileStatuses.updateVirusCheck(ctx.arg(FileIdArg), ctx.arg(VirusCheckStatusArg))
    ),
    Field("updateFileFormat",
      BooleanType,
      arguments = List(FileIdArg, PronomIdArg),
      resolve = ctx => ctx.ctx.fileFormats.create(ctx.arg(PronomIdArg), ctx.arg(FileIdArg))
    ),
    Field(
      "createMultipleFiles",
      ListType(FileType),
      arguments = List(MultipleFileInputsArg),
      resolve = ctx => ctx.ctx.files.createMultiple(ctx.arg(MultipleFileInputsArg))
    )
  ))

  private val schema = Schema(QueryType, Some(MutationType))

  private val seriesService = new SeriesService(new SeriesDao)
  private val consignmentService = new ConsignmentService(new ConsignmentDao, seriesService)
  private val fileStatusService = new FileStatusService(new FileStatusDao())
  private val fileFormatService = new FileFormatService(new FileFormatDao())
  private val fileService = new FileService(new FileDao, fileStatusService, consignmentService, fileFormatService)
  private val requestContext = new RequestContext(seriesService, consignmentService, fileService, fileStatusService, fileFormatService)


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
case class FileStatus(id: Int, clientSideChecksum: String, serverSideChecksum: String, fileFormatVerified: Boolean, fileId: Int, antivirusStatus: String)

case class File(id: Int, path: String, consignmentId: Int, fileStatus: FileStatus, pronomId: String)
case class CreateFileInput(path: String, consignmentId: Int)
