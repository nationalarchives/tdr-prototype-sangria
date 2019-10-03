package uk.gov.nationalarchives.tdr.api.core.db


// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = slick.jdbc.PostgresProfile
} with Tables
/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Array(Consignments.schema, File.schema, FileFormat.schema, FileMetadata.schema, FileStatus.schema, FlywaySchemaHistory.schema, Series.schema).reduceLeft(_ ++ _)
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table Consignments
    *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
    *  @param name Database column name SqlType(varchar), Length(50,true)
    *  @param seriesId Database column series_id SqlType(int4)
    *  @param creator Database column creator SqlType(varchar), Length(50,true)
    *  @param transferringBody Database column transferring_body SqlType(varchar), Length(50,true) */
  case class ConsignmentsRow(id: Int, name: String, seriesId: Int, creator: String, transferringBody: String)
  /** GetResult implicit for fetching ConsignmentsRow objects using plain SQL queries */
  implicit def GetResultConsignmentsRow(implicit e0: GR[Int], e1: GR[String]): GR[ConsignmentsRow] = GR{
    prs => import prs._
      ConsignmentsRow.tupled((<<[Int], <<[String], <<[Int], <<[String], <<[String]))
  }
  /** Table description of table consignments. Objects of this class serve as prototypes for rows in queries. */
  class Consignments(_tableTag: Tag) extends profile.api.Table[ConsignmentsRow](_tableTag, "consignments") {
    def * = (id, name, seriesId, creator, transferringBody) <> (ConsignmentsRow.tupled, ConsignmentsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(name), Rep.Some(seriesId), Rep.Some(creator), Rep.Some(transferringBody))).shaped.<>({r=>import r._; _1.map(_=> ConsignmentsRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column name SqlType(varchar), Length(50,true) */
    val name: Rep[String] = column[String]("name", O.Length(50,varying=true))
    /** Database column series_id SqlType(int4) */
    val seriesId: Rep[Int] = column[Int]("series_id")
    /** Database column creator SqlType(varchar), Length(50,true) */
    val creator: Rep[String] = column[String]("creator", O.Length(50,varying=true))
    /** Database column transferring_body SqlType(varchar), Length(50,true) */
    val transferringBody: Rep[String] = column[String]("transferring_body", O.Length(50,varying=true))

    /** Foreign key referencing Series (database name consignment_series_fk) */
    lazy val seriesFk = foreignKey("consignment_series_fk", seriesId, Series)(r => r.id, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table Consignments */
  lazy val Consignments = new TableQuery(tag => new Consignments(tag))

  /** Entity class storing rows of table File
    *  @param path Database column path SqlType(varchar), Length(5000,true)
    *  @param consignmentId Database column consignment_id SqlType(int4)
    *  @param fileSize Database column file_size SqlType(int4)
    *  @param fileName Database column file_name SqlType(varchar)
    *  @param id Database column id SqlType(uuid), PrimaryKey
    *  @param lastModifiedDate Database column last_modified_date SqlType(timestamptz) */
  case class FileRow(path: String, consignmentId: Int, fileSize: Int, fileName: String, id: java.util.UUID, lastModifiedDate: java.sql.Timestamp)
  /** GetResult implicit for fetching FileRow objects using plain SQL queries */
  implicit def GetResultFileRow(implicit e0: GR[String], e1: GR[Int], e2: GR[java.util.UUID], e3: GR[java.sql.Timestamp]): GR[FileRow] = GR{
    prs => import prs._
      FileRow.tupled((<<[String], <<[Int], <<[Int], <<[String], <<[java.util.UUID], <<[java.sql.Timestamp]))
  }
  /** Table description of table file. Objects of this class serve as prototypes for rows in queries. */
  class File(_tableTag: Tag) extends profile.api.Table[FileRow](_tableTag, "file") {
    def * = (path, consignmentId, fileSize, fileName, id, lastModifiedDate) <> (FileRow.tupled, FileRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(path), Rep.Some(consignmentId), Rep.Some(fileSize), Rep.Some(fileName), Rep.Some(id), Rep.Some(lastModifiedDate))).shaped.<>({r=>import r._; _1.map(_=> FileRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column path SqlType(varchar), Length(5000,true) */
    val path: Rep[String] = column[String]("path", O.Length(5000,varying=true))
    /** Database column consignment_id SqlType(int4) */
    val consignmentId: Rep[Int] = column[Int]("consignment_id")
    /** Database column file_size SqlType(int4) */
    val fileSize: Rep[Int] = column[Int]("file_size")
    /** Database column file_name SqlType(varchar) */
    val fileName: Rep[String] = column[String]("file_name")
    /** Database column id SqlType(uuid), PrimaryKey */
    val id: Rep[java.util.UUID] = column[java.util.UUID]("id", O.PrimaryKey)
    /** Database column last_modified_date SqlType(timestamptz) */
    val lastModifiedDate: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("last_modified_date")

    /** Foreign key referencing Consignments (database name file_consignment_id_fkey) */
    lazy val consignmentsFk = foreignKey("file_consignment_id_fkey", consignmentId, Consignments)(r => r.id, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table File */
  lazy val File = new TableQuery(tag => new File(tag))

  /** Entity class storing rows of table FileFormat
    *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
    *  @param pronomId Database column pronom_id SqlType(varchar), Length(255,true)
    *  @param fileId Database column file_id SqlType(uuid) */
  case class FileFormatRow(id: Int, pronomId: String, fileId: java.util.UUID)
  /** GetResult implicit for fetching FileFormatRow objects using plain SQL queries */
  implicit def GetResultFileFormatRow(implicit e0: GR[Int], e1: GR[String], e2: GR[java.util.UUID]): GR[FileFormatRow] = GR{
    prs => import prs._
      FileFormatRow.tupled((<<[Int], <<[String], <<[java.util.UUID]))
  }
  /** Table description of table file_format. Objects of this class serve as prototypes for rows in queries. */
  class FileFormat(_tableTag: Tag) extends profile.api.Table[FileFormatRow](_tableTag, "file_format") {
    def * = (id, pronomId, fileId) <> (FileFormatRow.tupled, FileFormatRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(pronomId), Rep.Some(fileId))).shaped.<>({r=>import r._; _1.map(_=> FileFormatRow.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column pronom_id SqlType(varchar), Length(255,true) */
    val pronomId: Rep[String] = column[String]("pronom_id", O.Length(255,varying=true))
    /** Database column file_id SqlType(uuid) */
    val fileId: Rep[java.util.UUID] = column[java.util.UUID]("file_id")

    /** Foreign key referencing File (database name file_format_file_uuid_fkey) */
    lazy val fileFk = foreignKey("file_format_file_uuid_fkey", fileId, File)(r => r.id, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table FileFormat */
  lazy val FileFormat = new TableQuery(tag => new FileFormat(tag))

  /** Entity class storing rows of table FileMetadata
    *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
    *  @param key Database column key SqlType(varchar), Length(50,true)
    *  @param value Database column value SqlType(varchar), Length(5000,true)
    *  @param fileId Database column file_id SqlType(uuid) */
  case class FileMetadataRow(id: Int, key: String, value: String, fileId: java.util.UUID)
  /** GetResult implicit for fetching FileMetadataRow objects using plain SQL queries */
  implicit def GetResultFileMetadataRow(implicit e0: GR[Int], e1: GR[String], e2: GR[java.util.UUID]): GR[FileMetadataRow] = GR{
    prs => import prs._
      FileMetadataRow.tupled((<<[Int], <<[String], <<[String], <<[java.util.UUID]))
  }
  /** Table description of table file_metadata. Objects of this class serve as prototypes for rows in queries. */
  class FileMetadata(_tableTag: Tag) extends profile.api.Table[FileMetadataRow](_tableTag, "file_metadata") {
    def * = (id, key, value, fileId) <> (FileMetadataRow.tupled, FileMetadataRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(key), Rep.Some(value), Rep.Some(fileId))).shaped.<>({r=>import r._; _1.map(_=> FileMetadataRow.tupled((_1.get, _2.get, _3.get, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column key SqlType(varchar), Length(50,true) */
    val key: Rep[String] = column[String]("key", O.Length(50,varying=true))
    /** Database column value SqlType(varchar), Length(5000,true) */
    val value: Rep[String] = column[String]("value", O.Length(5000,varying=true))
    /** Database column file_id SqlType(uuid) */
    val fileId: Rep[java.util.UUID] = column[java.util.UUID]("file_id")

    /** Foreign key referencing File (database name file_metadata_file_uuid_fkey) */
    lazy val fileFk = foreignKey("file_metadata_file_uuid_fkey", fileId, File)(r => r.id, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table FileMetadata */
  lazy val FileMetadata = new TableQuery(tag => new FileMetadata(tag))

  /** Entity class storing rows of table FileStatus
    *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
    *  @param fileFormatVerified Database column file_format_verified SqlType(bool)
    *  @param clientSideChecksum Database column client_side_checksum SqlType(varchar), Length(64,true), Default(None)
    *  @param serverSideChecksum Database column server_side_checksum SqlType(varchar), Length(64,true), Default(None)
    *  @param antivirusStatus Database column antivirus_status SqlType(varchar), Length(64,true), Default(None)
    *  @param fileId Database column file_id SqlType(uuid) */
  case class FileStatusRow(id: Int, fileFormatVerified: Boolean, clientSideChecksum: Option[String] = None, serverSideChecksum: Option[String] = None, antivirusStatus: Option[String] = None, fileId: java.util.UUID)
  /** GetResult implicit for fetching FileStatusRow objects using plain SQL queries */
  implicit def GetResultFileStatusRow(implicit e0: GR[Int], e1: GR[Boolean], e2: GR[Option[String]], e3: GR[java.util.UUID]): GR[FileStatusRow] = GR{
    prs => import prs._
      FileStatusRow.tupled((<<[Int], <<[Boolean], <<?[String], <<?[String], <<?[String], <<[java.util.UUID]))
  }
  /** Table description of table file_status. Objects of this class serve as prototypes for rows in queries. */
  class FileStatus(_tableTag: Tag) extends profile.api.Table[FileStatusRow](_tableTag, "file_status") {
    def * = (id, fileFormatVerified, clientSideChecksum, serverSideChecksum, antivirusStatus, fileId) <> (FileStatusRow.tupled, FileStatusRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(fileFormatVerified), clientSideChecksum, serverSideChecksum, antivirusStatus, Rep.Some(fileId))).shaped.<>({r=>import r._; _1.map(_=> FileStatusRow.tupled((_1.get, _2.get, _3, _4, _5, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column file_format_verified SqlType(bool) */
    val fileFormatVerified: Rep[Boolean] = column[Boolean]("file_format_verified")
    /** Database column client_side_checksum SqlType(varchar), Length(64,true), Default(None) */
    val clientSideChecksum: Rep[Option[String]] = column[Option[String]]("client_side_checksum", O.Length(64,varying=true), O.Default(None))
    /** Database column server_side_checksum SqlType(varchar), Length(64,true), Default(None) */
    val serverSideChecksum: Rep[Option[String]] = column[Option[String]]("server_side_checksum", O.Length(64,varying=true), O.Default(None))
    /** Database column antivirus_status SqlType(varchar), Length(64,true), Default(None) */
    val antivirusStatus: Rep[Option[String]] = column[Option[String]]("antivirus_status", O.Length(64,varying=true), O.Default(None))
    /** Database column file_id SqlType(uuid) */
    val fileId: Rep[java.util.UUID] = column[java.util.UUID]("file_id")

    /** Foreign key referencing File (database name file_status_file_uuid_fkey) */
    lazy val fileFk = foreignKey("file_status_file_uuid_fkey", fileId, File)(r => r.id, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table FileStatus */
  lazy val FileStatus = new TableQuery(tag => new FileStatus(tag))

  /** Entity class storing rows of table FlywaySchemaHistory
    *  @param installedRank Database column installed_rank SqlType(int4), PrimaryKey
    *  @param version Database column version SqlType(varchar), Length(50,true), Default(None)
    *  @param description Database column description SqlType(varchar), Length(200,true)
    *  @param `type` Database column type SqlType(varchar), Length(20,true)
    *  @param script Database column script SqlType(varchar), Length(1000,true)
    *  @param checksum Database column checksum SqlType(int4), Default(None)
    *  @param installedBy Database column installed_by SqlType(varchar), Length(100,true)
    *  @param installedOn Database column installed_on SqlType(timestamp)
    *  @param executionTime Database column execution_time SqlType(int4)
    *  @param success Database column success SqlType(bool) */
  case class FlywaySchemaHistoryRow(installedRank: Int, version: Option[String] = None, description: String, `type`: String, script: String, checksum: Option[Int] = None, installedBy: String, installedOn: java.sql.Timestamp, executionTime: Int, success: Boolean)
  /** GetResult implicit for fetching FlywaySchemaHistoryRow objects using plain SQL queries */
  implicit def GetResultFlywaySchemaHistoryRow(implicit e0: GR[Int], e1: GR[Option[String]], e2: GR[String], e3: GR[Option[Int]], e4: GR[java.sql.Timestamp], e5: GR[Boolean]): GR[FlywaySchemaHistoryRow] = GR{
    prs => import prs._
      FlywaySchemaHistoryRow.tupled((<<[Int], <<?[String], <<[String], <<[String], <<[String], <<?[Int], <<[String], <<[java.sql.Timestamp], <<[Int], <<[Boolean]))
  }
  /** Table description of table flyway_schema_history. Objects of this class serve as prototypes for rows in queries.
    *  NOTE: The following names collided with Scala keywords and were escaped: type */
  class FlywaySchemaHistory(_tableTag: Tag) extends profile.api.Table[FlywaySchemaHistoryRow](_tableTag, "flyway_schema_history") {
    def * = (installedRank, version, description, `type`, script, checksum, installedBy, installedOn, executionTime, success) <> (FlywaySchemaHistoryRow.tupled, FlywaySchemaHistoryRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(installedRank), version, Rep.Some(description), Rep.Some(`type`), Rep.Some(script), checksum, Rep.Some(installedBy), Rep.Some(installedOn), Rep.Some(executionTime), Rep.Some(success))).shaped.<>({r=>import r._; _1.map(_=> FlywaySchemaHistoryRow.tupled((_1.get, _2, _3.get, _4.get, _5.get, _6, _7.get, _8.get, _9.get, _10.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column installed_rank SqlType(int4), PrimaryKey */
    val installedRank: Rep[Int] = column[Int]("installed_rank", O.PrimaryKey)
    /** Database column version SqlType(varchar), Length(50,true), Default(None) */
    val version: Rep[Option[String]] = column[Option[String]]("version", O.Length(50,varying=true), O.Default(None))
    /** Database column description SqlType(varchar), Length(200,true) */
    val description: Rep[String] = column[String]("description", O.Length(200,varying=true))
    /** Database column type SqlType(varchar), Length(20,true)
      *  NOTE: The name was escaped because it collided with a Scala keyword. */
    val `type`: Rep[String] = column[String]("type", O.Length(20,varying=true))
    /** Database column script SqlType(varchar), Length(1000,true) */
    val script: Rep[String] = column[String]("script", O.Length(1000,varying=true))
    /** Database column checksum SqlType(int4), Default(None) */
    val checksum: Rep[Option[Int]] = column[Option[Int]]("checksum", O.Default(None))
    /** Database column installed_by SqlType(varchar), Length(100,true) */
    val installedBy: Rep[String] = column[String]("installed_by", O.Length(100,varying=true))
    /** Database column installed_on SqlType(timestamp) */
    val installedOn: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("installed_on")
    /** Database column execution_time SqlType(int4) */
    val executionTime: Rep[Int] = column[Int]("execution_time")
    /** Database column success SqlType(bool) */
    val success: Rep[Boolean] = column[Boolean]("success")

    /** Index over (success) (database name flyway_schema_history_s_idx) */
    val index1 = index("flyway_schema_history_s_idx", success)
  }
  /** Collection-like TableQuery object for table FlywaySchemaHistory */
  lazy val FlywaySchemaHistory = new TableQuery(tag => new FlywaySchemaHistory(tag))

  /** Entity class storing rows of table Series
    *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
    *  @param name Database column name SqlType(varchar), Length(50,true)
    *  @param description Database column description SqlType(varchar), Length(4000,true) */
  case class SeriesRow(id: Int, name: String, description: String)
  /** GetResult implicit for fetching SeriesRow objects using plain SQL queries */
  implicit def GetResultSeriesRow(implicit e0: GR[Int], e1: GR[String]): GR[SeriesRow] = GR{
    prs => import prs._
      SeriesRow.tupled((<<[Int], <<[String], <<[String]))
  }
  /** Table description of table series. Objects of this class serve as prototypes for rows in queries. */
  class Series(_tableTag: Tag) extends profile.api.Table[SeriesRow](_tableTag, "series") {
    def * = (id, name, description) <> (SeriesRow.tupled, SeriesRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(name), Rep.Some(description))).shaped.<>({r=>import r._; _1.map(_=> SeriesRow.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column name SqlType(varchar), Length(50,true) */
    val name: Rep[String] = column[String]("name", O.Length(50,varying=true))
    /** Database column description SqlType(varchar), Length(4000,true) */
    val description: Rep[String] = column[String]("description", O.Length(4000,varying=true))
  }
  /** Collection-like TableQuery object for table Series */
  lazy val Series = new TableQuery(tag => new Series(tag))
}
