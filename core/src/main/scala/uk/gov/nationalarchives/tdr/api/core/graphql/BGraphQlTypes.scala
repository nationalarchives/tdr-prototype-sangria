package uk.gov.nationalarchives.tdr.api.core.graphql

import java.time._
import java.util.UUID

import cats.syntax.either._
import io.circe.Decoder
import sangria.ast.StringValue
import sangria.macros.derive._
import sangria.schema.{Field, InputObjectType, IntType, ListType, ObjectType, OptionType, ScalarType, Schema, StringType, fields}
import sangria.validation.ValueCoercionViolation
import uk.gov.nationalarchives.tdr.api.core.db.dao._
import uk.gov.nationalarchives.tdr.api.core.graphql.service._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

object BGraphQlTypes {


  private val seriesService = new SeriesService(new SeriesDao)
  private val consignmentService = new ConsignmentService(new ConsignmentDao, seriesService)
  private val fileStatusService = new FileStatusService(new FileStatusDao())
  private val fileFormatService = new FileFormatService(new FileFormatDao())
  private val fileService = new FileService(new FileDao, fileStatusService, consignmentService, fileFormatService)
  private val requestContext = new RequestContext(seriesService, consignmentService, fileService, fileStatusService, fileFormatService)

  private case object UuidCoercionViolation extends ValueCoercionViolation("Valid UUID expected")

  private case object InstantCoercionViolation extends ValueCoercionViolation("UTC Instant value expected")

  private def parseUuid(s: String): Either[ValueCoercionViolation, UUID] = Try(UUID.fromString(s)) match {
    case Success(uuid) => Right(uuid)
    case Failure(_) => Left(UuidCoercionViolation)
  }

  implicit private val UuidType: ScalarType[UUID] = ScalarType[UUID]("UUID",
    coerceOutput = (u, _) => u.toString,
    coerceUserInput = {
      case s: String => parseUuid(s)
      case _ => Left(UuidCoercionViolation)
    },
    coerceInput = {
      case StringValue(s, _, _, _, _) => parseUuid(s)
      case _ => Left(UuidCoercionViolation)
    }
  )

  implicit private val localDateTimeDecoder: Decoder[Instant] = Decoder.decodeString.emap(str =>
    Either.catchNonFatal(Instant.parse(str)).leftMap(t => "LocalDateTime")
  )

  private def parseInstant(s: String) = Try(
    Instant.parse(s)) match {
    case Success(instant) ⇒ Right(instant)
    case Failure(_) ⇒ Left(InstantCoercionViolation)
  }

  implicit private val InstantType = ScalarType[Instant](
    "Instant",
    coerceOutput = (i, _) => i.toString,
    coerceUserInput = {
      case s: String => parseInstant(s)
      case _ => Left(InstantCoercionViolation)
    },
    coerceInput = {
      case StringValue(s, _, _, _, _) => parseInstant(s)
      case _ => Left(InstantCoercionViolation)
    }
  )

  implicit private val SeriesType: ObjectType[Unit, Series] = deriveObjectType[Unit, Series]()
  implicit private val CreateSeriesInputType: InputObjectType[CreateSeriesInput] = deriveInputObjectType[CreateSeriesInput]()
  implicit private val FileStatusType: ObjectType[Unit, FileStatus] = deriveObjectType[Unit, FileStatus]()
  implicit private val CreateFileInputType: InputObjectType[CreateFileInput] = deriveInputObjectType[CreateFileInput]()
  implicit private val FileCheckStatusType: ObjectType[Unit, FileCheckStatus] = deriveObjectType[Unit, FileCheckStatus]()

  implicit private val FileType: ObjectType[Unit, File] = ObjectType(
    "File",
    fields[Unit, File](
      Field("id", UuidType, resolve = _.value.id),
      Field("path", StringType, resolve = _.value.path),
      Field("consignmentId", IntType, resolve = _.value.consignmentId),
      Field(
        "fileStatus",
        FileStatusType,
        resolve = context => DeferFileStatus(context.value.id)
      ),
      Field("pronomId", OptionType(StringType), resolve = _.value.pronomId),
      Field("fileSize", IntType, resolve = _.value.fileSize),
      Field("lastModifiedDate", InstantType, resolve = _.value.lastModifiedDate),
      Field("fileName", StringType, resolve = _.value.fileName),
    )
  )
  implicit private val ConsignmentType: ObjectType[Unit, Consignment] = ObjectType(
    "Consignment",
    fields[Unit, Consignment](
      Field("id", IntType, resolve = _.value.id),
      Field("name", StringType, resolve = _.value.name),
      Field("creator", StringType, resolve = _.value.creator),
      Field("transferringBody", StringType, resolve = _.value.transferringBody),
      Field("series", SeriesType, resolve = _.value.series),
      Field(
        "files",
        ListType(FileType),
        resolve = context => DeferConsignmentFiles(context.value.id)
      )
    )
  )



  trait Query {

    @GraphQLField
    def getAllSeries = {
     requestContext.series.all
    }

    @GraphQLField
    def getConsignment(id: Int) = {
      requestContext.consignments.get(id)
    }

    @GraphQLField
    def getConsignments = {
      requestContext.consignments.all
    }

    @GraphQLField
    def getFile(id:UUID) = {
      requestContext.files.get(id)
    }

    @GraphQLField
    def getFiles = {
      requestContext.files.all
    }

    @GraphQLField
    def getFileChecksStatus(id:Int) = {
      requestContext.fileStatuses.getFileCheckStatus(id)
    }
  }


  trait Mutation {
    @GraphQLField
    def createConsignment(name: String, seriesId: Int, creator: String, transferringBody: String) = {
      requestContext.consignments.create(name, seriesId, creator, transferringBody)
    }

    @GraphQLField
    def createSeries(name: String, description: String) = {
      requestContext.series.create(CreateSeriesInput(name,description))
    }

    @GraphQLField
    def createFile(path:String, consignmentId:Int, fileSize:Int, lastModifiedDate:Instant,fileName:String, clientSideChecksum: String) = {
      requestContext.files.create(CreateFileInput(path, consignmentId, fileSize, lastModifiedDate,fileName,clientSideChecksum))
    }


//    Field(
//      "createConsignment",
//      ConsignmentType,
//      arguments = List(ConsignmentNameArg, SeriesIdArg, ConsignmentCreatorArg, ConsignmentTransferringBodyArg),
//      resolve = ctx => ctx.ctx.consignments.create(ctx.arg(ConsignmentNameArg), ctx.arg(SeriesIdArg), ctx.arg(ConsignmentCreatorArg), ctx.arg(ConsignmentTransferringBodyArg)))
//    private val ConsignmentNameArg = Argument("name", StringType)
//    private val ConsignmentCreatorArg = Argument("creator", StringType)
//    private val ConsignmentTransferringBodyArg = Argument("transferringBody", StringType)
//    private val SeriesIdArg = Argument("seriesId", IntType)
  }

  case class MyCtx(mutation: Mutation, query: Query, series: SeriesService, consignments: ConsignmentService, files: FileService,  fileStatuses: FileStatusService, fileFormats: FileFormatService)

  val context = MyCtx(new Mutation {}, new Query {},seriesService, consignmentService, fileService, fileStatusService, fileFormatService)
  val MutationType: ObjectType[MyCtx, Unit] = deriveContextObjectType[MyCtx, Mutation, Unit](_.mutation)
  val QueryType = deriveContextObjectType[MyCtx, Query, Unit](_.query)
  val schema: Schema[MyCtx, Unit] = Schema(QueryType, Some(MutationType))
}




