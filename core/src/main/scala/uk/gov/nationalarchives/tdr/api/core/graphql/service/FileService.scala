package uk.gov.nationalarchives.tdr.api.core.graphql.service

import uk.gov.nationalarchives.tdr.api.core
import uk.gov.nationalarchives.tdr.api.core.File
import uk.gov.nationalarchives.tdr.api.core.db.dao.FileDao
import uk.gov.nationalarchives.tdr.api.core.db.model.FileRow

import scala.concurrent.{ExecutionContext, Future}

class FileService(fileDao: FileDao, fileStatusService: FileStatusService, consignmentService: ConsignmentService)(implicit val executionContext: ExecutionContext) {

  def all: Future[Seq[File]] = {
    fileDao.all.flatMap(fileRows => {
      val files = fileRows.map(fileRow =>
        consignmentService.get(fileRow.consignmentId).map(consignment =>
          core.File(fileRow.id.get, fileRow.path, consignment.get.id)
        )
      )
      Future.sequence(files)
    })
  }

  def get(id: Int): Future[Option[File]] = {
    fileDao.get(id).flatMap(_.map(fileRow =>
      consignmentService.get(fileRow.consignmentId).map(consignment =>
        core.File(fileRow.id.get, fileRow.path, consignment.get.id)
      )
    ) match {
      case Some(f) => f.map(Some(_))
      case None => Future.successful(None)
    })
  }

  def create(path: String, consignmentId: Int): Future[File] = {
    val newFile = FileRow(None, path, consignmentId)
    val result = fileDao.create(newFile)

    for {
      persistedFile <- result
      _ <- fileStatusService.create(persistedFile.id.get)
    } yield
      core.File(persistedFile.id.get, persistedFile.path, consignmentId)

  }
}
