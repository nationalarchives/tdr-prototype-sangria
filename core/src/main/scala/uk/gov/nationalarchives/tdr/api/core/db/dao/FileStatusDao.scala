package uk.gov.nationalarchives.tdr.api.core.db.dao

import java.util.UUID

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag
import uk.gov.nationalarchives.tdr.api.core.db.DbConnection
import uk.gov.nationalarchives.tdr.api.core.db.dao.ConsignmentDao.consignments
import uk.gov.nationalarchives.tdr.api.core.db.dao.FileDao.files
import uk.gov.nationalarchives.tdr.api.core.db.dao.FileFormatDao.fileFormats
import uk.gov.nationalarchives.tdr.api.core.db.dao.FileStatusDao.fileStatuses
import uk.gov.nationalarchives.tdr.api.core.db.model.{FileRow, FileStatus}
import uk.gov.nationalarchives.tdr.api.core.graphql.FileCheckStatus

import scala.concurrent.{ExecutionContext, Future}

class FileStatusDao(implicit val executionContext: ExecutionContext) {
  private val db = DbConnection.db

  def all: Future[Seq[FileRow]] = {
    db.run(files.result)
  }

  private val insert = fileStatuses returning fileStatuses.map(_.id) into ((fileStatus, id) => fileStatus.copy(id = Some(id)))

  def create(fileId: UUID, clientSideChecksum: String): Future[FileStatus] = {
    val fileStatus = FileStatus(None, false, fileId, clientSideChecksum, "", "")
    db.run(insert += fileStatus)
  }

  def createMultiple(fileStatusRows: Seq[FileStatus]): Future[Seq[FileStatus]] = {
    db.run(insert ++= fileStatusRows)
  }

  def get(id: Int): Future[Option[FileStatus]] = {
    db.run(fileStatuses.filter(_.id === id).result).map(_.headOption)
  }

  def getByFileId(fileId: UUID): Future[Option[FileStatus]] = {
    db.run(fileStatuses.filter(_.fileId === fileId).result).map(_.headOption)
  }

  case class FileCheck(clientChecksum: String, serverChecksum: String, virusStatus: String, pronomId: Option[String])

  case class FileStatusCount(completeCount: Int, error: Boolean)

  def getFileCheckStatus(consignmentId: Int) = {
    val query = for {
      ((c, fs), ff) <- files join fileStatuses on (_.id === _.fileId) joinLeft fileFormats on (_._2.fileId === _.fileId)
    } yield (fs.clientSideChecksum, fs.serverSideChecksum, fs.antivirus_status, ff.map(_.pronomId), fs.fileId)

    def boolToInt(b: Boolean): Int = if (b) 1 else 0

    println(query.result.statements)
    val result = db.run(query.result)

    val checkList: Future[Seq[FileCheck]] = result.map(_.map(f => FileCheck(f._1, f._2, f._3, f._4)))


    val fn: (FileStatusCount, FileCheck) => FileStatusCount = (acc, s) => {
      val isCompleteCount = acc.completeCount + boolToInt(s.serverChecksum.length > 0 && s.virusStatus.length > 0 && s.pronomId.getOrElse("").length > 0)
      val error = acc.error || (s.virusStatus != "OK" && s.virusStatus.nonEmpty) || (s.serverChecksum != s.clientChecksum)
      FileStatusCount(isCompleteCount, error)
    }
    val results: Future[FileStatusCount] = checkList.map(_.foldLeft(FileStatusCount(0, false))(fn))
    results.onComplete(println(_))

    for {
      r <- result
      fsc <- results
    } yield {
      FileCheckStatus(fsc.completeCount, r.length, fsc.error)
    }

  }

  def updateServerSideChecksum(fileId: UUID, checksum: String) = {
    val q = for {c <- fileStatuses if c.fileId === fileId} yield c.serverSideChecksum
    val updateAction = q.update(checksum)
    db.run(updateAction)
  }

  def updateClientSideChecksum(fileId: UUID, checksum: String) = {
    val q = for {c <- fileStatuses if c.fileId === fileId} yield c.clientSideChecksum
    val updateAction = q.update(checksum)
    db.run(updateAction)
  }

  def updateVirusCheckStatus(fileId: UUID, virusCheckStatus: String) = {
    val q = for {c <- fileStatuses if c.fileId === fileId} yield c.antivirus_status
    val updateAction = q.update(virusCheckStatus)
    db.run(updateAction)
  }
}

object FileStatusDao {
  val fileStatuses = TableQuery[FileStatusTable]
}

class FileStatusTable(tag: Tag) extends Table[FileStatus](tag, "file_status") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

  def fileFormatVerified = column[Boolean]("file_format_verified")

  def fileId = column[UUID]("file_id")

  def clientSideChecksum = column[String]("client_side_checksum")

  def serverSideChecksum = column[String]("server_side_checksum")

  def antivirus_status = column[String]("antivirus_status")

  def file = foreignKey("file_file_status_fk", fileId, files)(_.id)

  override def * = (id.?, fileFormatVerified, fileId, clientSideChecksum, serverSideChecksum, antivirus_status).mapTo[FileStatus]
}