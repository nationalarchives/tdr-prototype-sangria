package uk.gov.nationalarchives.tdr.api.core.db.dao

import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID

import slick.jdbc.PostgresProfile.api._
import slick.lifted.{TableQuery, Tag}
import uk.gov.nationalarchives.tdr.api.core.db.DbConnection
import uk.gov.nationalarchives.tdr.api.core.db.dao.ConsignmentDao.consignments
import uk.gov.nationalarchives.tdr.api.core.db.dao.FileDao.files
import uk.gov.nationalarchives.tdr.api.core.db.model.FileRow

import scala.concurrent.{ExecutionContext, Future}

class FileDao(implicit val executionContext: ExecutionContext) {
  private val db = DbConnection.db

  private val insertQuery = files returning files.map(_.id) into ((file, id) => file.copy(id = Some(id)))

  def all: Future[Seq[FileRow]] = {
    db.run(files.result)
  }

  def get(id: UUID): Future[Option[FileRow]] = {
    db.run(files.filter(_.id === id).result).map(_.headOption)
  }

  def create(file: FileRow): Future[FileRow] = {
    db.run(insertQuery += file)
  }
}

object FileDao {
  val files = TableQuery[FileTable]
}

class FileTable(tag: Tag) extends Table[FileRow](tag, "file") {
  implicit private val dateColumnType = MappedColumnType.base[LocalDateTime, Timestamp](
    ldt => Timestamp.valueOf(ldt),
    ts => ts.toLocalDateTime
  )

  def id = column[UUID]("id", O.PrimaryKey, O.AutoInc)
  def path = column[String]("path")
  def consignmentId = column[Int]("consignment_id")
  def fileSize = column[Int]("file_size")
  def lastModifiedDate = column[LocalDateTime]("last_modified_date")
  def fileName = column[String]("file_name")
  def consignment = foreignKey("file_consignment_fk", consignmentId, consignments)(_.id)

  override def * = (id.?, path, consignmentId, fileSize, lastModifiedDate, fileName).mapTo[FileRow]
}
