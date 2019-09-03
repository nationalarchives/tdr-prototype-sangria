package uk.gov.nationalarchives.tdr.api.core.graphql.service

import java.util.UUID

import uk.gov.nationalarchives.tdr.api.core.db.dao.FileFormatDao

import scala.concurrent.{ExecutionContext, Future}

class FileFormatService(fileFormatDao: FileFormatDao)(implicit val executionContext: ExecutionContext) {

  def create(pronomId: String, fileId: UUID): Future[Boolean] = {
    fileFormatDao.createOrUpdate(pronomId, fileId).map(_ => true)
  }

  def getByFileId(fileId: UUID) = {
    fileFormatDao.getByFileId(fileId)
  }

}
