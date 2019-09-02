package uk.gov.nationalarchives.tdr.api.core.graphql.service

import uk.gov.nationalarchives.tdr.api.core.db.dao.FileStatusDao
import uk.gov.nationalarchives.tdr.api.core.graphql.FileStatus

import scala.concurrent.{ExecutionContext, Future}

class FileStatusService(fileStatusDao: FileStatusDao)(implicit val executionContext: ExecutionContext) {
  def updateServerSideChecksum(id: Int, checksum: String): Future[Boolean] = {
    fileStatusDao.updateServerSideChecksum(id, checksum).map(a => {
      if(a != 1) {
        throw new RuntimeException("Too many or not enough rows")
      }
      true
    })
  }

  def updateClientSideChecksum(id: Int, checksum: String): Future[Boolean] = {
    fileStatusDao.updateClientSideChecksum(id, checksum).map(a => {
      if(a != 1) {
        throw new RuntimeException("Too many or not enough rows")
      }
      true
    })
  }


  def updateVirusCheck(id: Int, virusCheckStatus: String): Future[Boolean] = {
    fileStatusDao.updateVirusCheckStatus(id, virusCheckStatus).map(a => {
      if(a != 1) {
        throw new RuntimeException("Too many or not enough rows")
      }
      true
    })
  }

  def create(fileId: Int, clientSideChecksum: String): Future[FileStatus] = {
    fileStatusDao.create(fileId, clientSideChecksum)
      .map(fs => FileStatus(fs.id.get, fs.clientSideChecksum, fs.serverSideChecksum, fs.fileFormatVerified, fs.fileId, fs.antivirusStatus))
  }
}
