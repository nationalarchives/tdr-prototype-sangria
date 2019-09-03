package uk.gov.nationalarchives.tdr.api.core.graphql.service

import uk.gov.nationalarchives.tdr.api.core.db.dao.FileFormatDao

import scala.concurrent.{ExecutionContext, Future}

class FileFormatService(fileFormatDao: FileFormatDao)(implicit val executionContext: ExecutionContext) {

  def create(pronomId: String, fileId: Int): Future[Boolean] = {
    fileFormatDao.createOrUpdate(pronomId, fileId).map(_ => true)
  }

  def getByFileId(fileId: Int) = {
    fileFormatDao.getByFileId(fileId)
  }

}
