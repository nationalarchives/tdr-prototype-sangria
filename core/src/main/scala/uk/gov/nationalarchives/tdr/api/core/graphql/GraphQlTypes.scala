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
  implicit private val FileStatusType: ObjectType[Unit, FileStatus] = deriveObjectType[Unit, FileStatus]()
  implicit private val CreateFileInputType: InputObjectType[CreateFileInput] = deriveInputObjectType[CreateFileInput]()
  implicit private val FileCheckStatusType: ObjectType[Unit, FileCheckStatus] = deriveObjectType[Unit, FileCheckStatus]()
  implicit private val UserType: ObjectType[Unit, User] = deriveObjectType[Unit, User]()
  implicit private val PasswordInfoType: ObjectType[Unit, PasswordInfo] = deriveObjectType[Unit, PasswordInfo]()
  implicit private val UserDataType: InputObjectType[UserInput] = deriveInputObjectType[UserInput]()
  implicit private val PasswordInputType: InputObjectType[PasswordInput] = deriveInputObjectType[PasswordInput]()
  implicit private val PasswordResetType: ObjectType[Unit, PasswordResetToken] = deriveObjectType[Unit, PasswordResetToken]()
  implicit private val TotpType: ObjectType[Unit, TotpScratchCodesOuput] = deriveObjectType[Unit, TotpScratchCodesOuput]()
  implicit private val TotpScratchCodesInputType: InputObjectType[TotpScratchCodesInput] = deriveInputObjectType[TotpScratchCodesInput]()
  implicit private val TotpInfoInputType: InputObjectType[TotpInfoInput] = deriveInputObjectType[TotpInfoInput]()


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

  implicit private val TotpInfoType: ObjectType[Unit, TotpInfoOutput] = ObjectType(
    "TotpInfo",
    fields[Unit, TotpInfoOutput](
      Field("id", IntType, resolve = _.value.id),
      Field("providerKey", StringType, resolve = _.value.providerKey),
      Field("sharedKey", StringType, resolve = _.value.sharedKey),
      Field("scratchCodes", ListType(TotpType), resolve = context => DeferScratchCodes(context.value.id))
    )
  )

  private val ConsignmentNameArg = Argument("name", StringType)
  private val ConsignmentIdArg = Argument("id", IntType)
  private val ConsignmentCreatorArg = Argument("creator", StringType)
  private val ConsignmentTransferringBodyArg = Argument("transferringBody", StringType)
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
  private val ProviderKeyArg = Argument("providerKey", StringType)
  private val ProviderIdArg = Argument("providerId", StringType)
  private val EmailArg = Argument("email", StringType)
  private val TokenArg = Argument("token", StringType)
  private val UserDataArg = Argument("userData", UserDataType)
  private val PasswordInputArg = Argument("passwordInput", PasswordInputType)
  private val TotpArg = Argument("totp", TotpInfoInputType)

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
      "getSeriesForCreator",
      OptionType(SeriesType),
      arguments = List(SeriesIdArg, ConsignmentCreatorArg),
      resolve = ctx => ctx.ctx.series.get(ctx.arg(SeriesIdArg), ctx.arg(ConsignmentCreatorArg))
    ),
    Field(
      "getConsignmentForCreator",
      OptionType(ConsignmentType),
      arguments = List(ConsignmentIdArg, ConsignmentCreatorArg),
      resolve = ctx => ctx.ctx.consignments.get(ctx.arg(ConsignmentIdArg), ctx.arg(ConsignmentCreatorArg))
    ),
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
      resolve = ctx => ctx.ctx.files.all),
    Field(
      "getFileChecksStatus",
      FileCheckStatusType,
      arguments = List(ConsignmentIdArg),
      resolve = ctx => ctx.ctx.fileStatuses.getFileCheckStatus(ctx.arg(ConsignmentIdArg))
    ),
    Field(
      "getUser",
      OptionType(UserType),
      arguments = List(ProviderKeyArg, ProviderIdArg),
      resolve = ctx => ctx.ctx.users.get(ctx.arg(ProviderKeyArg), ctx.arg(ProviderIdArg))
    ),
    Field(
      "findPassword",
      OptionType(PasswordInfoType),
      arguments = List(ProviderKeyArg),
      resolve = ctx => ctx.ctx.users.findPassword(ctx.arg(ProviderKeyArg))
    ),
    Field(
      "findTotp",
      OptionType(TotpInfoType),
      arguments = List(ProviderKeyArg),
      resolve = ctx => ctx.ctx.users.findTotp(ctx.arg(ProviderKeyArg))
    ),
    Field(
      "isPasswordTokenValid",
      BooleanType,
      arguments = List(EmailArg, TokenArg),
      resolve = ctx => ctx.ctx.users.isPasswordResetTokenValid(ctx.arg(EmailArg), ctx.arg(TokenArg))
    )
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
      arguments = List(ConsignmentNameArg, SeriesIdArg, ConsignmentCreatorArg, ConsignmentTransferringBodyArg),
      resolve = ctx => ctx.ctx.consignments.create(ctx.arg(ConsignmentNameArg), ctx.arg(SeriesIdArg), ctx.arg(ConsignmentCreatorArg), ctx.arg(ConsignmentTransferringBodyArg))),
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
    ),
    Field(
      "createUser",
      OptionType(UserType),
      arguments = List(UserDataArg),
      resolve = ctx => ctx.ctx.users.create(ctx.arg(UserDataArg))
    ),
    Field(
      "addPassword",
      OptionType(PasswordInfoType),
      arguments = List(PasswordInputArg),
      resolve = ctx => ctx.ctx.users.addPassword(ctx.arg(PasswordInputArg))
    ),
    Field(
      "updatePassword",
      IntType,
      arguments = List(PasswordInputArg),
      resolve = ctx => ctx.ctx.users.updatePassword(ctx.arg(PasswordInputArg))
    ),
      Field(
      "removePassword",
      IntType,
      arguments = List(ProviderKeyArg),
      resolve = ctx => ctx.ctx.users.deletePassword(ctx.arg(ProviderKeyArg))
    ),
    Field(
      "addTotp",
      OptionType(TotpInfoType),
      arguments = List(TotpArg),
      resolve = ctx => ctx.ctx.users.addTotp(ctx.arg(TotpArg))
    ),
    Field(
      "updateTotp",
      IntType,
      arguments = List(TotpArg),
      resolve = ctx => ctx.ctx.users.updateTotp(ctx.arg(TotpArg))
    ),
    Field(
      "removeTotp",
      IntType,
      arguments = List(ProviderKeyArg),
      resolve = ctx => ctx.ctx.users.deleteTotp(ctx.arg(ProviderKeyArg))
    ),
    Field(
      "createPasswordResetToken",
      OptionType(PasswordResetType),
      arguments = List(EmailArg),
      resolve = ctx => ctx.ctx.users.createResetPasswordToken(ctx.arg(EmailArg))
    )
  ))

  val schema: Schema[RequestContext, Unit] = Schema(QueryType, Some(MutationType))
}

case class Series(id: Int, name: String, description: String)
case class CreateSeriesInput(name: String, description: String)
case class Consignment(id: Int, name: String, series: Series, creator: String, transferringBody: String)
case class FileStatus(id: Int, clientSideChecksum: String, serverSideChecksum: String, fileFormatVerified: Boolean, fileId: UUID, antivirusStatus: String)
//TODO: need to define a custom scalar date type to store dates in DB

case class File(id: UUID, path: String, consignmentId: Int, fileStatus: FileStatus, pronomId: Option[String], fileSize: Int, lastModifiedDate: Instant, fileName: String)
case class CreateFileInput(path: String, consignmentId: Int, fileSize: Int, lastModifiedDate: Instant, fileName: String, clientSideChecksum: String)
case class FileCheckStatus(totalComplete: Int, totalFiles: Int, error: Boolean)
case class ConsignmentInput(name: String, series: Series, creator: String, transferringBody:String)
case class User(id: Int, firstName: String, lastName: String, email: String, providerId: String, providerKey: String)
case class UserInput(firstName: String, lastName: String, email: String, providerId: String)
case class PasswordInfo(hasher: String,password: String,salt: Option[String])
case class PasswordInput(providerKey: String, hasher: String,password: String,salt: Option[String])
case class PasswordResetToken(email: String, token: String)
case class TotpInfoOutput(id: Int, providerKey: String, sharedKey: String, scratchCodes: Seq[TotpScratchCodesOuput])
case class TotpScratchCodesOuput(id: Int, hasher: String, password: String, salt: Option[String])
case class TotpInfoInput(providerKey: String, sharedKey: String, scratchCodes: Seq[TotpScratchCodesInput])
case class TotpScratchCodesInput(hasher: String, password: String, salt: Option[String])

