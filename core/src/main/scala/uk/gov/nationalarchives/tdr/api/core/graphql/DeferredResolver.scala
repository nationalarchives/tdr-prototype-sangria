package uk.gov.nationalarchives.tdr.api.core.graphql

import java.util.UUID

import sangria.execution.deferred.{Deferred, UnsupportedDeferError}

import scala.concurrent.{ExecutionContext, Future}

class DeferredResolver extends sangria.execution.deferred.DeferredResolver[RequestContext] {
  override def resolve(deferred: Vector[Deferred[Any]], context: RequestContext, queryState: Any)(implicit ec: ExecutionContext): Vector[Future[Any]] = {
    deferred.map {
      case DeferFileStatus(fileId) => context.fileStatuses.getByFileId(fileId).map(status => status.get)
      case DeferScratchCodes(infoId) => context.users.findTotpScratchCodes(infoId)
      case DeferConsignmentFilesOffsetPagination(consignmentId, limit, offset) => context.files.getOffsetPagination(consignmentId, limit, offset)
      case DeferConsignmentFilesKeySetPagination(consignmentId, limit, currentCursor) => context.files.getKeySetPagination(consignmentId, limit, currentCursor)
      case other => throw UnsupportedDeferError(other)
    }
  }
}

case class DeferScratchCodes(infoId: Int) extends Deferred[List[TotpScratchCodesOuput]]
case class DeferFileStatus(fileId: UUID) extends Deferred[FileStatus]
case class DeferConsignmentFilesOffsetPagination(consignmentId: Int, limit: Int, offset: Int) extends Deferred[(Int, Vector[FileEdge])]
case class DeferConsignmentFilesKeySetPagination(consignmentId: Int, limit: Int, currentCursor: String) extends Deferred[(String, Vector[FileEdge])]
