package uk.gov.nationalarchives.tdr.api.core.graphql.service

import uk.gov.nationalarchives.tdr.api.core
import uk.gov.nationalarchives.tdr.api.core.db.dao.FileDao
import uk.gov.nationalarchives.tdr.api.core.db.model.FileRow
import uk.gov.nationalarchives.tdr.api.core.{CreateFileInput, File}

import scala.concurrent.{ExecutionContext, Future}

class FileService(fileDao: FileDao, fileStatusService: FileStatusService, consignmentService: ConsignmentService, fileFormatService: FileFormatService)(implicit val executionContext: ExecutionContext) {

  def all: Future[Seq[File]] = {
    fileDao.all.flatMap(fileRows => {
      val files = fileRows.map(fileRow =>
        consignmentService.get(fileRow.consignmentId).map(consignment =>
          core.File(fileRow.id.get, fileRow.path, consignment.get.id, null, null, fileRow.fileSize, fileRow.lastModifiedDate,
            fileRow.fileName)
        )
      )
      Future.sequence(files)
    })
  }


  def get(id: Int) = {
    for {
      fileOption <- fileDao.get(id)
      file <- fileOption.map(Future.successful).getOrElse(Future.failed(new Exception))
      fileStatusOption <- fileStatusService.getByFileId(fileOption.get.id.get)
      fileStatus <- fileStatusOption.map(Future.successful).getOrElse(Future.failed(new Exception))
      fileFormat <- fileFormatService.getByFileId(file.id.get)
    } yield core.File(file.id.get, file.path, file.consignmentId,
      core.FileStatus(fileStatus.id.get, fileStatus.clientSideChecksum, fileStatus.serverSideChecksum, fileStatus.fileFormatVerified, fileStatus.fileId, fileStatus.antivirusStatus),
      fileFormat.map(_.pronomId)
      , file.fileSize, file.lastModifiedDate,file.fileName
    )
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
      fileStatus <- fileStatusService.create(persistedFile.id.get, input.clientSideChecksum)
    } yield
      core.File(persistedFile.id.get, persistedFile.path, input.consignmentId,
        core.FileStatus(fileStatus.id, fileStatus.clientSideChecksum, fileStatus.serverSideChecksum, fileStatus.fileFormatVerified, fileStatus.fileId, fileStatus.antivirusStatus)
        ,null, persistedFile.fileSize, persistedFile.lastModifiedDate, persistedFile.fileName
      )


  }
}
