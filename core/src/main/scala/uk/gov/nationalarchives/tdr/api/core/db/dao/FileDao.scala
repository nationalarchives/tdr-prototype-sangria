package uk.gov.nationalarchives.tdr.api.core.db.dao

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import slick.jdbc.GetResult
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{TableQuery, Tag}
import uk.gov.nationalarchives.tdr.api.core.db.DbConnection
import uk.gov.nationalarchives.tdr.api.core.db.dao.ConsignmentDao.consignments
import uk.gov.nationalarchives.tdr.api.core.db.dao.FileDao.files
import uk.gov.nationalarchives.tdr.api.core.db.model.FileRow
import uk.gov.nationalarchives.tdr.api.core.graphql.CreateFileInput

import scala.concurrent.{ExecutionContext, Future}

class FileDao(implicit val executionContext: ExecutionContext) {

  private val db = DbConnection.db

  private val insertQuery = files returning files.map(_.id) into ((file, id) => file.copy(id = Some(id)))

  case class FileDbRow(path: String, consignmentId: Int, fileSize: Int, fileName: String, id: Option[UUID] = None, lastModifiedDate: Timestamp)

  def all: Future[Seq[FileRow]] = {
    db.run(files.result)
  }

  def get(id: UUID): Future[Option[FileRow]] = {
    db.run(files.filter(_.id === id).result).map(_.headOption)
  }

  def getNumberOfFilesByConsignment(consignmentId: Int): Future[Int] = {
    db.run(files.filter(_.consignmentId === consignmentId).size.result)
  }

  def getByConsignment(consignmentId: Int): Future[Seq[FileRow]] = {
    db.run(files.filter(_.consignmentId === consignmentId).result)
  }

  /*
  * This is sub-optimal as need to get the full results for each consignment to return the total count
  * Total count needed to calculate whether more pages available
  * Need to store the total number of files in the consignment table
  * */
  def getKeySetPagination(consignmentId: Int, limit: Int, after: String): Future[Seq[FileRow]] = {
    val criteriaConsignment = Option(consignmentId)
    val criteriaAfter = Option(after)

    val cns = files.filter { f =>
      List(
        criteriaConsignment.map(f.consignmentId === _),
        criteriaAfter.map(f.path > _))
      .collect({case Some(criteria) => criteria})
      .reduceLeftOption(_ && _).getOrElse(true: Rep[Boolean])
    }.sortBy(_.path).take(limit)

    db.run(cns.result)
  }

  def getOffsetPagination(consignmentId: Int, limit: Int, offset: Int): Future[Seq[FileRow]] = {
    db.run (files.filter(_.consignmentId === consignmentId).sortBy(_.path).drop(offset).take(limit).result)
  }

  def create(file: FileRow): Future[FileRow] = {
    db.run(insertQuery += file)
  }

  def createMultiple(inputs: Seq[CreateFileInput]): Future[Seq[FileRow]] = {
    val fileRows: Seq[FileRow] = inputs.map(i => FileRow(None, i.path, i.consignmentId, i.fileSize, i.lastModifiedDate, i.fileName))
    db.run(insertQuery ++= fileRows)
  }
}

object FileDao {
  val files = TableQuery[FileTable]
}

class FileTable(tag: Tag) extends Table[FileRow](tag, "file") {
  implicit private val dateColumnType = MappedColumnType.base[Instant, Timestamp](
    i => Timestamp.from(i),
    ts => ts.toInstant
  )

  def id = column[UUID]("id", O.PrimaryKey, O.AutoInc)
  def path = column[String]("path")
  def consignmentId = column[Int]("consignment_id")
  def fileSize = column[Int]("file_size")
  def lastModifiedDate = column[Instant]("last_modified_date")
  def fileName = column[String]("file_name")
  def consignment = foreignKey("file_consignment_fk", consignmentId, consignments)(_.id)

  //mapTo function displaying error in Intellij but not causing any problems with compilation and running
  //Likely a bug with Intellij error detection
  override def * = (id.?, path, consignmentId, fileSize, lastModifiedDate, fileName).mapTo[FileRow]
}
