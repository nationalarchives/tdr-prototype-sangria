package uk.gov.nationalarchives.tdr.api.core.graphql.service

import java.util.UUID

import uk.gov.nationalarchives.tdr.api.core.db.dao.FileStatusDao
import uk.gov.nationalarchives.tdr.api.core.db.model.{FileRow, FileStatusRow}
import uk.gov.nationalarchives.tdr.api.core.graphql.{CreateFileInput, FileCheckStatus, FileStatus}

import scala.concurrent.{ExecutionContext, Future}

class FileStatusService(fileStatusDao: FileStatusDao)(implicit val executionContext: ExecutionContext) {

  def createMutiple(pathToInput: Map[String, CreateFileInput], result: Seq[FileRow]): Future[Seq[FileStatusRow]] = {
    val fileStatusRows = result.map(r => FileStatusRow(None, false, r.id.get, pathToInput(r.path).clientSideChecksum, "", ""))
    fileStatusDao.createMultiple(fileStatusRows)
  }

  def updateServerSideChecksum(fileId: UUID, checksum: String): Future[Boolean] = {
    fileStatusDao.updateServerSideChecksum(fileId, checksum).map(a => {
      if (a != 1) {
        throw new RuntimeException("Too many or not enough rows")
      }
      true
    })
  }

  def getFileCheckStatus(consignmentId: Int): Future[FileCheckStatus] = {
    fileStatusDao.getFileCheckStatus(consignmentId)
  }

  def updateClientSideChecksum(fileId: UUID, checksum: String): Future[Boolean] = {
    fileStatusDao.updateClientSideChecksum(fileId, checksum).map(a => {
      if (a != 1) {
        throw new RuntimeException("Too many or not enough rows")
      }
      true
    })
  }


  def updateVirusCheck(fileId: UUID, virusCheckStatus: String): Future[Boolean] = {
    fileStatusDao.updateVirusCheckStatus(fileId, virusCheckStatus).map(a => {
      if (a != 1) {
        throw new RuntimeException("Too many or not enough rows")
      }
      true
    })
  }

  def create(fileId: UUID, clientSideChecksum: String): Future[FileStatusRow] = {
    fileStatusDao.create(fileId, clientSideChecksum)
  }

  def getByFileId(fileId: UUID): Future[Option[FileStatus]] = {
    fileStatusDao.getByFileId(fileId).map(_.map(fileStatus =>
      FileStatus(fileStatus.id.get, fileStatus.clientSideChecksum, fileStatus.serverSideChecksum, fileStatus.fileFormatVerified, fileStatus.fileId, fileStatus.antivirusStatus)
    ))
  }
}
