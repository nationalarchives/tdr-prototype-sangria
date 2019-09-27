package uk.gov.nationalarchives.tdr.api.core.graphql

import sangria.execution.deferred.{Deferred, UnsupportedDeferError}

import scala.concurrent.{ExecutionContext, Future}

class DeferredResolver extends sangria.execution.deferred.DeferredResolver[RequestContext] {
  override def resolve(deferred: Vector[Deferred[Any]], context: RequestContext, queryState: Any)(implicit ec: ExecutionContext): Vector[Future[Any]] = {
    deferred.map {
      case DeferConsignmentFiles(consignmentId) => context.files.getByConsignment(consignmentId)
      case other => throw UnsupportedDeferError(other)
    }
  }
}

case class DeferConsignmentFiles(consignmentId: Int) extends Deferred[List[File]]
