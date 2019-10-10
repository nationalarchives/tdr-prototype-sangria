package uk.gov.nationalarchives.tdr.api.core.db.dao

import java.util.UUID

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag
import uk.gov.nationalarchives.tdr.api.core.db.DbConnection
import uk.gov.nationalarchives.tdr.api.core.db.dao.FileDao.files
import uk.gov.nationalarchives.tdr.api.core.db.dao.FileFormatDao.fileFormats
import uk.gov.nationalarchives.tdr.api.core.db.dao.FileStatusDao.fileStatuses
import uk.gov.nationalarchives.tdr.api.core.db.model.{FileRow, FileStatusRow}
import uk.gov.nationalarchives.tdr.api.core.graphql.FileCheckStatus

import scala.concurrent.{ExecutionContext, Future}

class FileStatusDao(implicit val executionContext: ExecutionContext) {
  private val db = DbConnection.db

  def all: Future[Seq[FileRow]] = {
    db.run(files.result)
  }

  private val insert = fileStatuses returning fileStatuses.map(_.id) into ((fileStatus, id) => fileStatus.copy(id = Some(id)))

  def create(fileId: UUID, clientSideChecksum: String): Future[FileStatusRow] = {
    val fileStatus = FileStatusRow(None, fileFormatVerified = false, fileId = fileId, clientSideChecksum = clientSideChecksum, serverSideChecksum = "", antivirusStatus = "")
    db.run(insert += fileStatus)
  }

  def createMultiple(fileStatusRows: Seq[FileStatusRow]): Future[Seq[FileStatusRow]] = {
    db.run(insert ++= fileStatusRows)
  }

  def get(id: Int): Future[Option[FileStatusRow]] = {
    db.run(fileStatuses.filter(_.id === id).result).map(_.headOption)
  }

  def getByFileId(fileId: UUID): Future[Option[FileStatusRow]] = {
    db.run(fileStatuses.filter(_.fileId === fileId).result).map(_.headOption)
  }

  case class FileCheck(clientChecksum: String, serverChecksum: String, virusStatus: String, pronomId: Option[String], fileName: String)


  case class FileStatusCount(completeCount: Int, virusErrors: List[String], checksumErrors: List[String])

  def getFileCheckStatus(consignmentId: Int): Future[FileCheckStatus] = {
    val query = for {
      ((c, fs), ff) <- files join fileStatuses on (_.id === _.fileId) filter(_._1.consignmentId === consignmentId) joinLeft fileFormats on (_._2.fileId === _.fileId)
    } yield (fs.clientSideChecksum, fs.serverSideChecksum, fs.antivirus_status, ff.map(_.pronomId), c.path)


    def boolToInt(b: Boolean): Int = if (b) 1 else 0



    println(query.result.statements)
    val result = db.run(query.result)

    val checkList: Future[Seq[FileCheck]] = result.map(_.map(f => FileCheck(f._1, f._2, f._3, f._4, f._5)))

    val fn: (FileStatusCount, FileCheck) => FileStatusCount = (acc, s) => {
      val isCompleteCount = acc.completeCount + boolToInt(s.serverChecksum.length > 0) + boolToInt(s.virusStatus.length > 0) + boolToInt(s.pronomId.getOrElse("").length > 0)

      val hasVirusError = s.virusStatus != "OK" && s.virusStatus.nonEmpty
      val hasChecksumError: Boolean =  s.serverChecksum != s.clientChecksum && s.serverChecksum.nonEmpty

      val getErrors: (Boolean, String) => List[String] = (errorCheck, fileName) => {
        if (errorCheck) {
          List(fileName)
        } else {
          List()
        }
      }

      s match {
        case _ if hasVirusError || hasChecksumError =>
          FileStatusCount(isCompleteCount, acc.virusErrors ::: getErrors(hasVirusError, s.fileName),
            acc.checksumErrors ::: getErrors(hasChecksumError, s.fileName))
        case _ => FileStatusCount(isCompleteCount, acc.virusErrors, acc.checksumErrors)
      }
    }
    val results: Future[FileStatusCount] = checkList.map(_.foldLeft(FileStatusCount(0, List(), List()))(fn))
    results.onComplete(println(_))

    for {
      r <- result
      fsc <- results
    } yield {
      val percentage: Int = r.length match {
        case 0 => throw new RuntimeException("No files found")
        case l =>((fsc.completeCount.toDouble / (l * 3).toDouble) * 100).toInt match {
          case i if i < 5 => 5
          case i => i
        }
      }
      FileCheckStatus(percentage, r.length, fsc.virusErrors, fsc.checksumErrors)
    }

  }

  def updateServerSideChecksum(fileId: UUID, checksum: String): Future[Int] = {
    val q = for {c <- fileStatuses if c.fileId === fileId} yield c.serverSideChecksum
    val updateAction = q.update(checksum)
    db.run(updateAction)
  }

  def updateClientSideChecksum(fileId: UUID, checksum: String): Future[Int] = {
    val q = for {c <- fileStatuses if c.fileId === fileId} yield c.clientSideChecksum
    val updateAction = q.update(checksum)
    db.run(updateAction)
  }

  def updateVirusCheckStatus(fileId: UUID, virusCheckStatus: String): Future[Int] = {
    val q = for {c <- fileStatuses if c.fileId === fileId} yield c.antivirus_status
    val updateAction = q.update(virusCheckStatus)
    db.run(updateAction)
  }
}

object FileStatusDao {
  val fileStatuses = TableQuery[FileStatusTable]
}

class FileStatusTable(tag: Tag) extends Table[FileStatusRow](tag, "file_status") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def fileFormatVerified = column[Boolean]("file_format_verified")
  def fileId = column[UUID]("file_id")
  def clientSideChecksum = column[String]("client_side_checksum")
  def serverSideChecksum = column[String]("server_side_checksum")
  def antivirus_status = column[String]("antivirus_status")
  def file = foreignKey("file_file_status_fk", fileId, files)(_.id)

  override def * = (id.?, fileFormatVerified, fileId, clientSideChecksum, serverSideChecksum, antivirus_status).mapTo[FileStatusRow]
}