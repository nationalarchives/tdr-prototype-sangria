package uk.gov.nationalarchives.tdr.api.core.graphql.service
import java.util.UUID

import sangria.relay.util.Base64
import uk.gov.nationalarchives.tdr.api.core.db.dao.FileDao
import uk.gov.nationalarchives.tdr.api.core.db.model.{FileRow, FileStatusRow}
import uk.gov.nationalarchives.tdr.api.core.graphql
import uk.gov.nationalarchives.tdr.api.core.graphql.{CreateFileInput, File, FileEdge, FileStatus}

import scala.concurrent.{ExecutionContext, Future}

class FileService(fileDao: FileDao, fileStatusService: FileStatusService, consignmentService: ConsignmentService, fileFormatService: FileFormatService)(implicit val executionContext: ExecutionContext) {

  def all: Future[Seq[File]] = {
    fileDao.all.flatMap(fileRows => mapFileRows(fileRows))
  }

  def get(id: UUID) = {
    for {
      fileOption <- fileDao.get(id)
      file <- fileOption.map(Future.successful).getOrElse(Future.failed(new Exception))
      fileStatus <- fileStatusService.getByFileId(fileOption.get.id.get)
      fileFormat <- fileFormatService.getByFileId(file.id.get)
    } yield File(file.id.get, file.path, file.consignmentId,
      fileStatus.get,
      fileFormat.map(_.pronomId)
      , file.fileSize, file.lastModifiedDate, file.fileName
    )
  }

  def getByConsignment(consignmentId: Int): Future[Seq[File]] = {
    fileDao.getByConsignment(consignmentId).flatMap(fileRows => mapFileRows(fileRows))
  }

  def getOffsetPagination(consignmentId: Int, limit: Int, offset: Int): Future[(Int, Seq[FileEdge])] = {
    for {
      response <- fileDao.getOffsetPagination(consignmentId, limit, offset)
      paginatedFiles <- mapToEdges(response)
      count <- fileDao.getNumberOfFilesByConsignment(consignmentId)
    } yield (count, paginatedFiles)
  }

  def getKeySetPagination(consignmentId: Int, limit: Int, after: String): Future[(String, Seq[FileEdge])] = {
      for {
        response <- fileDao.getKeySetPagination(consignmentId, limit + 1, after)
        nextCursor = Base64.encode(response.last.path)
        //count <- fileDao.getNumberOfFilesByConsignment(consignmentId)
        paginatedFiles <- mapToEdges(response.dropRight(1))
      } yield (nextCursor, paginatedFiles)
  }

  def createMultiple(inputs: Seq[graphql.CreateFileInput]): Future[Seq[File]] = {
    val pathToInput: Map[String, CreateFileInput] = inputs.groupBy(_.path).mapValues(_.head)

    for {
      result <- fileDao.createMultiple(inputs)
      fileStatuses <- fileStatusService.createMultiple(pathToInput, result)
    } yield {

      val fileIdToStatus: Map[UUID, FileStatusRow] = fileStatuses.groupBy(_.fileId).mapValues(_.head)
      result.map(r => {
        val fileStatus: FileStatusRow = fileIdToStatus(r.id.get)
        getFileReturnValue(r, fileStatus)
      }
      )
    }
  }

  private def getFileReturnValue(r: FileRow, fileStatus: FileStatusRow) = {
    val returnFileStatus = FileStatus(fileStatus.id.get, fileStatus.clientSideChecksum, fileStatus.serverSideChecksum, fileStatus.fileFormatVerified, r.id.get, fileStatus.antivirusStatus)
    File(r.id.get,
      r.path,
      r.consignmentId,
      returnFileStatus,
      Option.apply(""),
      r.fileSize,
      r.lastModifiedDate, r.fileName)
  }

  private def mapToEdges(fileRows: Seq[FileRow]) = {
    mapFileRows(fileRows).map(fs => {
      fs.map(f => FileEdge(f, Base64.encode(f.path)))
    })
  }

  private def mapFileRows(fileRows: Seq[FileRow]): Future[Seq[File]] = {
    val files = fileRows.map(fileRow =>
      consignmentService.get(fileRow.consignmentId).map(consignment =>
        File(fileRow.id.get, fileRow.path, consignment.get.id, null, null, fileRow.fileSize, fileRow.lastModifiedDate, fileRow.fileName)
      )
    )
    Future.sequence(files)
  }

  def create(input: graphql.CreateFileInput): Future[File] = {
    val newFile = FileRow(None, input.path, input.consignmentId, input.fileSize, input.lastModifiedDate, input.fileName)
    val result = fileDao.create(newFile)

    for {
      persistedFile <- result
      fileStatus <- fileStatusService.create(persistedFile.id.get, input.clientSideChecksum)
    } yield
      getFileReturnValue(persistedFile, fileStatus)
  }
}
