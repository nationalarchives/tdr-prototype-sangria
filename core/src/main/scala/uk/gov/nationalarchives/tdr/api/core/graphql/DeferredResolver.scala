package uk.gov.nationalarchives.tdr.api.core.graphql

import java.util.UUID

import sangria.execution.deferred.{Deferred, UnsupportedDeferError}

import scala.concurrent.{ExecutionContext, Future}

class DeferredResolver extends sangria.execution.deferred.DeferredResolver[RequestContext] {
  override def resolve(deferred: Vector[Deferred[Any]], context: RequestContext, queryState: Any)(implicit ec: ExecutionContext): Vector[Future[Any]] = {
    deferred.map {
      case DeferConsignmentFiles(consignmentId) => context.files.getByConsignment(consignmentId)
      case DeferFileStatus(fileId) => context.fileStatuses.getByFileId(fileId).map(status => status.get)
      case DeferScratchCodes(infoId) => context.users.findTotpScratchCodes(infoId)
      case DeferOffsetPagination(consignmentId, limit, offset) => context.files.getOffsetPagination(consignmentId, limit, offset)
      case DeferKeySetPagination(consignmentId, limit, after) => context.files.getKeySetPagination(consignmentId, limit, after)
      case other => throw UnsupportedDeferError(other)
    }
  }
}

case class DeferConsignmentFiles(consignmentId: Int) extends Deferred[List[File]]
case class DeferScratchCodes(infoId: Int) extends Deferred[List[TotpScratchCodesOuput]]
case class DeferFileStatus(fileId: UUID) extends Deferred[FileStatus]
case class DeferOffsetPagination(consignmentId: Int, limit: Int, offset: Int) extends Deferred[(Int, Vector[FileEdge])]
case class DeferKeySetPagination(consignmentId: Int, limit: Int, after: String) extends Deferred[(Int, Vector[FileEdge])]
