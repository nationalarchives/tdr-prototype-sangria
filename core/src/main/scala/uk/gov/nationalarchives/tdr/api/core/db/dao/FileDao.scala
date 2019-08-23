package uk.gov.nationalarchives.tdr.api.core.db.dao

import slick.jdbc.PostgresProfile.api._
import slick.lifted.{TableQuery, Tag}
import uk.gov.nationalarchives.tdr.api.core.db.DbConnection
import uk.gov.nationalarchives.tdr.api.core.db.dao.FileDao.files
import uk.gov.nationalarchives.tdr.api.core.db.model.{FileRow}
import uk.gov.nationalarchives.tdr.api.core.db.dao.ConsignmentDao.consignments

import scala.concurrent.{ExecutionContext, Future}

class FileDao(implicit val executionContext: ExecutionContext) {
  private val db = DbConnection.db

  private val insertQuery = files returning files.map(_.id) into ((file, id) => file.copy(id = Some(id)))

  def all: Future[Seq[FileRow]] = {
    db.run(files.result)
  }

  def get(id: Int): Future[Option[FileRow]] = {
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
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def path = column[String]("path")
  def consignmentId = column[Int]("consignment_id")
  def consignment = foreignKey("file_consignment_fk", consignmentId, consignments)(_.id)

  override def * = (id.?, path, consignmentId).mapTo[FileRow]
}
