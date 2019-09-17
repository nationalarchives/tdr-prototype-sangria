package uk.gov.nationalarchives.tdr.api.core.graphql.service

import java.util.UUID

import uk.gov.nationalarchives.tdr.api.core.db.dao.FileStatusDao
import uk.gov.nationalarchives.tdr.api.core.db.model
import uk.gov.nationalarchives.tdr.api.core.db.model.{FileRow, FileStatus}
import uk.gov.nationalarchives.tdr.api.core.graphql.CreateFileInput

import scala.concurrent.{ExecutionContext, Future}

class FileStatusService(fileStatusDao: FileStatusDao)(implicit val executionContext: ExecutionContext) {


  def createMutiple(pathToInput: Map[String, CreateFileInput], result: Seq[FileRow]): Future[Seq[FileStatus]] = {
    val fileStatusRows = result.map(r => model.FileStatus(None, false, r.id.get, pathToInput(r.path).clientSideChecksum, "", ""))
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
  }

  def getByFileId(fileId: UUID): Future[Option[model.FileStatus]] = {
    fileStatusDao.getByFileId(fileId)
  }
}
