package uk.gov.nationalarchives.tdr.api.core.graphql.service

import uk.gov.nationalarchives.tdr.api.core
import uk.gov.nationalarchives.tdr.api.core.{CreateFileInput, File}
import uk.gov.nationalarchives.tdr.api.core.db.dao.{ConsignmentDao, FileDao}
import uk.gov.nationalarchives.tdr.api.core.db.model.{ConsignmentRow, FileRow}

import scala.concurrent.{ExecutionContext, Future}

class FileService(fileDao: FileDao, consignmentService: ConsignmentService)(implicit val executionContext: ExecutionContext) {

  def all: Future[Seq[File]] = {
    fileDao.all.flatMap(fileRows => {
      val files = fileRows.map(fileRow =>
        consignmentService.get(fileRow.consignmentId).map(consignment =>
          core.File(fileRow.id.get, fileRow.path, consignment.get)
        )
      )
      Future.sequence(files)
    })
  }

  def get(id: Int): Future[Option[File]] = {
    fileDao.get(id).flatMap(_.map(fileRow =>
      consignmentService.get(fileRow.consignmentId).map(consignment =>
        core.File(fileRow.id.get, fileRow.path, consignment.get)
      )
    ) match {
      case Some(f) => f.map(Some(_))
      case None => Future.successful(None)
    })
  }

  def createMultiple(inputs: Seq[CreateFileInput]): Future[Seq[File]] = {
    val files = inputs.map(
      input => {
        create(input)
      }
    )
    Future.sequence(files)
  }

  def create(input: CreateFileInput): Future[File] = {
    val newFile = FileRow(None, input.path, input.consignmentId)
    val result = fileDao.create(newFile)

    result.flatMap(persistedFile =>
      consignmentService.get(persistedFile.consignmentId).map(consignment =>
        core.File(persistedFile.id.get, persistedFile.path, consignment.get)
      )
    )
  }
}
