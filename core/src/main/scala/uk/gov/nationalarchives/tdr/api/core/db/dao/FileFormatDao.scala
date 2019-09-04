package uk.gov.nationalarchives.tdr.api.core.db.dao

import java.util.UUID

import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._
import uk.gov.nationalarchives.tdr.api.core.db.DbConnection
import uk.gov.nationalarchives.tdr.api.core.db.model.{FileFormat, FileRow}
import uk.gov.nationalarchives.tdr.api.core.db.dao.FileDao.files
import uk.gov.nationalarchives.tdr.api.core.db.dao.FileFormatDao.fileFormats

import scala.concurrent.{ExecutionContext, Future}

class FileFormatDao(implicit val executionContext: ExecutionContext) {
  private val db = DbConnection.db

  private val insertQuery = fileFormats returning fileFormats.map(_.id) into ((fileFormat, id) => fileFormat.copy(id = Some(id)))


  def getByFileId(fileId: UUID): Future[Option[FileFormat]] = {
    db.run(fileFormats.filter(_.fileId === fileId).result).map(_.headOption)
  }

  def createOrUpdate(pronomId: String, fileId: UUID) = {
    getByFileId(fileId).map(fileFormat => {
      if(fileFormat.isEmpty) {
        val fileFormat: FileFormat = FileFormat(null, pronomId, fileId)
        db.run(insertQuery += fileFormat)
      } else {
        val q = for { c <- fileFormats if c.fileId === fileId } yield c.pronomId
        val updateAction = q.update(pronomId)
        db.run(updateAction)
      }
    })

  }
}

object FileFormatDao {
  val fileFormats = TableQuery[FileFormatTable]
}

class FileFormatTable(tag: Tag) extends Table[FileFormat](tag, "file_format") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def pronomId = column[String]("pronom_id")
  def fileId = column[UUID]("file_id")
  def file = foreignKey("file_format_file_fk", fileId, files)(_.id)

  override def * = (id.?, pronomId, fileId).mapTo[FileFormat]
}