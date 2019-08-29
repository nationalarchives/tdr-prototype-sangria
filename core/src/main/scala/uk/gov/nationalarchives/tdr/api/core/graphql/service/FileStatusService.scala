package uk.gov.nationalarchives.tdr.api.core.graphql.service

import uk.gov.nationalarchives.tdr.api.core.FileStatus
import uk.gov.nationalarchives.tdr.api.core.db.dao.FileStatusDao

import scala.concurrent.{ExecutionContext, Future}

class FileStatusService(fileStatusDao: FileStatusDao)(implicit val executionContext: ExecutionContext) {
  def updateServerSideChecksum(id: Int, checksum: String): Future[Boolean] = {
    fileStatusDao.updateServerSideChecksum(id, checksum).map(a => {
      println(a)
      if(a != 1) {
        throw new RuntimeException("Too many or not enough rows")
      }
      true
    })
  }

  def updateClientSideChecksum(id: Int, checksum: String): Future[Boolean] = {
    fileStatusDao.updateClientSideChecksum(id, checksum).map(a => {
      println(a)
      if(a != 1) {
        throw new RuntimeException("Too many or not enough rows")
      }
      true
    })
  }


  def updateVirusCheck(id: Int, virusCheckStatus: String): Future[Boolean] = {
    fileStatusDao.updateVirusCheckStatus(id, virusCheckStatus).map(a => {
      println(a)
      if(a != 1) {
        throw new RuntimeException("Too many or not enough rows")
      }
      true
    })
  }

  def create(fileId: Int): Future[FileStatus] = {
    fileStatusDao.create(fileId)
      .map(fs => FileStatus(fs.id.get, fs.clientSideChecksum, fs.serverSideChecksum, fs.fileFormatVerified, fs.fileId, fs.antivirusStatus))
  }
}
