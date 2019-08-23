package uk.gov.nationalarchives.tdr.api.core.db.dao

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag
import uk.gov.nationalarchives.tdr.api.core.db.DbConnection
import uk.gov.nationalarchives.tdr.api.core.db.model.{FileRow, FileStatus}
import uk.gov.nationalarchives.tdr.api.core.db.dao.FileDao.files
import uk.gov.nationalarchives.tdr.api.core.db.dao.FileStatusDao.fileStatuses

import scala.concurrent.{ExecutionContext, Future}

class FileStatusDao(implicit val executionContext: ExecutionContext) {
  private val db = DbConnection.db

  def all: Future[Seq[FileRow]] = {
    db.run(files.result)
  }

  private val insert = fileStatuses returning fileStatuses.map(_.id) into ((fileStatus, id) => fileStatus.copy(id = Some(id)))

  def create(fileId: Int): Future[FileStatus] = {
    val fileStatus = FileStatus(None, false, false, fileId, "", "")
    db.run(insert += fileStatus)
  }


  def get(id: Int): Future[Option[FileStatus]] = {
    db.run(fileStatuses.filter(_.id === id).result).map(_.headOption)
  }

  def updateChecksum(id: Int, checksum: String) = {
    val q = for { c <- fileStatuses if c.fileId === id } yield c.serverSideChecksum
    val updateAction = q.update(checksum)
    db.run(updateAction)
  }
}

object FileStatusDao {
  val fileStatuses = TableQuery[FileStatusTable]
}

class FileStatusTable(tag: Tag) extends Table[FileStatus](tag, "file_status") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def antiVirusPassed = column[Boolean]("antivirus_passed")
  def fileFormatVerified = column[Boolean]("file_format_verified")
  def fileId = column[Int]("file_id")
  def clientSideChecksum = column[String]("client_side_checksum")
  def serverSideChecksum = column[String]("server_side_checksum")
  def file = foreignKey("file_file_status_fk", fileId, files)(_.id)

  override def * = (id.?, antiVirusPassed, fileFormatVerified, fileId, clientSideChecksum, serverSideChecksum).mapTo[FileStatus]
}