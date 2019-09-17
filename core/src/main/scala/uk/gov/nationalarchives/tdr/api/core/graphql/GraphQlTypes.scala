package uk.gov.nationalarchives.tdr.api.core.graphql

import java.time._
import java.util.UUID

import cats.syntax.either._
import io.circe.Decoder
import io.circe.generic.auto._
import sangria.ast.StringValue
import sangria.macros.derive._
import sangria.marshalling.circe._
import sangria.schema.{Argument, BooleanType, Field, InputObjectType, IntType, ListInputType, ListType, ObjectType, OptionType, ScalarType, Schema, StringType, fields}
import sangria.validation.ValueCoercionViolation

import scala.util.{Failure, Success, Try}

object GraphQlTypes {

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
  implicit private val ConsignmentType: ObjectType[Unit, Consignment] = deriveObjectType[Unit, Consignment]()
  implicit private val FileType: ObjectType[Unit, File] = deriveObjectType[Unit, File]()
  implicit private val FileStatusType: ObjectType[Unit, FileStatus] = deriveObjectType[Unit, FileStatus]()
  implicit private val CreateFileInputType: InputObjectType[CreateFileInput] = deriveInputObjectType[CreateFileInput]()

  private val ConsignmentNameArg = Argument("name", StringType)
  private val ConsignmentIdArg = Argument("id", IntType)
  private val SeriesIdArg = Argument("seriesId", IntType)
  private val SeriesNameArg = Argument("name", StringType)
  private val SeriesDescriptionArg = Argument("description", StringType)
  private val SeriesInputArg = Argument("createSeriesInput", CreateSeriesInputType)
  private val FileIdArg = Argument("id", UuidType)
  private val ChecksumArg = Argument("checksum", StringType)
  private val VirusCheckStatusArg = Argument("status", StringType)
  private val PronomIdArg = Argument("pronomId", StringType)
  private val FileInputArg = Argument("createFileInput", CreateFileInputType)
  private val MultipleFileInputsArg = Argument("createFileInputs", ListInputType(CreateFileInputType))

  private val QueryType = ObjectType("Query", fields[RequestContext, Unit](
    Field(
      "getAllSeries",
      ListType(SeriesType),
      resolve = ctx => ctx.ctx.series.all),
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
      "createSeries",
      SeriesType,
      arguments = List(SeriesInputArg),
      resolve = ctx => ctx.ctx.series.create(ctx.arg(SeriesInputArg))
    ),
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

  val schema: Schema[RequestContext, Unit] = Schema(QueryType, Some(MutationType))
}

case class Series(id: Int, name: String, description: String)
case class CreateSeriesInput(name: String, description: String)
case class Consignment(id: Int, name: String, series: Series)
case class FileStatus(id: Int, clientSideChecksum: String, serverSideChecksum: String, fileFormatVerified: Boolean, fileId: UUID, antivirusStatus: String)
//TODO: need to define a custom scalar date type to store dates in DB
case class File(id: UUID, path: String, consignmentId: Int, fileStatus: FileStatus, pronomId: Option[String], fileSize: Int, lastModifiedDate: Instant, fileName: String)
case class CreateFileInput(path: String, consignmentId: Int, fileSize: Int, lastModifiedDate: Instant, fileName: String, clientSideChecksum: String)

