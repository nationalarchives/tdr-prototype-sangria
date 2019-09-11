package uk.gov.nationalarchives.tdr.api.core.graphql.service

import java.util.UUID

import uk.gov.nationalarchives.tdr.api.core.db.dao.FileStatusDao
import uk.gov.nationalarchives.tdr.api.core.db.model
import uk.gov.nationalarchives.tdr.api.core.graphql.{FileCheckStatus, FileStatus}

import scala.concurrent.{ExecutionContext, Future}

class FileStatusService(fileStatusDao: FileStatusDao)(implicit val executionContext: ExecutionContext) {
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

  def create(fileId: UUID, clientSideChecksum: String): Future[FileStatus] = {
    fileStatusDao.create(fileId, clientSideChecksum)
      .map(fs => FileStatus(fs.id.get, fs.clientSideChecksum, fs.serverSideChecksum, fs.fileFormatVerified, fs.fileId, fs.antivirusStatus))
  }

  def getByFileId(fileId: UUID): Future[Option[model.FileStatus]] = {
    fileStatusDao.getByFileId(fileId)
  }
}
