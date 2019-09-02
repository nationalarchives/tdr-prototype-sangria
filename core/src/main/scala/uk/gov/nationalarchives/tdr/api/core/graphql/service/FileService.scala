package uk.gov.nationalarchives.tdr.api.core.graphql.service

import uk.gov.nationalarchives.tdr.api.core

import uk.gov.nationalarchives.tdr.api.core.{CreateFileInput, File}
import uk.gov.nationalarchives.tdr.api.core.db.dao.{FileDao}
import uk.gov.nationalarchives.tdr.api.core.db.model.{FileRow}

import scala.concurrent.{ExecutionContext, Future}

class FileService(fileDao: FileDao, fileStatusService: FileStatusService, consignmentService: ConsignmentService)(implicit val executionContext: ExecutionContext) {

  def all: Future[Seq[File]] = {
    fileDao.all.flatMap(fileRows => {
      val files = fileRows.map(fileRow =>
        consignmentService.get(fileRow.consignmentId).map(consignment =>
          core.File(fileRow.id.get, fileRow.path, consignment.get.id, fileRow.fileSize, fileRow.lastModifiedDate,
            fileRow.fileName)
        )
      )
      Future.sequence(files)
    })
  }

  def get(id: Int): Future[Option[File]] = {
    fileDao.get(id).flatMap(_.map(fileRow =>
      consignmentService.get(fileRow.consignmentId).map(consignment =>
        core.File(fileRow.id.get, fileRow.path, consignment.get.id, fileRow.fileSize, fileRow.lastModifiedDate,
          fileRow.fileName)
      )
    ) match {
      case Some(f) => f.map(Some(_))
      case None => Future.successful(None)
    })
  }

  def createMultiple(inputs: Seq[CreateFileInput]): Future[Seq[File]] = {
    //TODO: this should be a sql that adds mutliple rows instead of iterating
    val files = inputs.map(
      input => {
        create(input)
      }
    )
    Future.sequence(files)
  }

  def create(input: CreateFileInput): Future[File] = {
    val newFile = FileRow(None, input.path, input.consignmentId, input.fileSize, input.lastModifiedDate, input.fileName)
    val result = fileDao.create(newFile)

    for {
      persistedFile <- result
      _ <- fileStatusService.create(persistedFile.id.get, input.clientSideChecksum)
    } yield
      core.File(persistedFile.id.get, persistedFile.path, persistedFile.consignmentId, persistedFile.fileSize, persistedFile.lastModifiedDate, persistedFile.fileName)

  }
}
