package uk.gov.nationalarchives.tdr.api.core.graphql.service

import uk.gov.nationalarchives.tdr.api.core.db.dao.ConsignmentDao

import uk.gov.nationalarchives.tdr.api.core.graphql.Consignment
import uk.gov.nationalarchives.tdr.api.core.db.Tables._

import scala.concurrent.{ExecutionContext, Future}

class ConsignmentService(consignmentDao: ConsignmentDao, seriesService: SeriesService)(implicit val executionContext: ExecutionContext) {

  def all: Future[Seq[Consignment]] = {
    consignmentDao.all.flatMap(consignmentRows => {
      val consignments = consignmentRows.map(consignmentRow =>
        seriesService.get(consignmentRow.seriesId).map(series =>
          Consignment(consignmentRow.id, consignmentRow.name, series.get, consignmentRow.creator, consignmentRow.transferringBody)
        )
      )
      Future.sequence(consignments)
    })
  }

  def get(id: Int): Future[Option[Consignment]] = {
    consignmentDao.get(id).flatMap(_.map(consignmentRow =>
      seriesService.get(consignmentRow.seriesId).map(series =>
        Consignment(consignmentRow.id, consignmentRow.name, series.get, consignmentRow.creator, consignmentRow.transferringBody)
      )
    ) match {
      case Some(f) => f.map(Some(_))
      case None => Future.successful(None)
    })
  }

  def create(name: String, seriesId: Int, creator: String, transferringBody: String): Future[Consignment] = {
    val newConsignment = ConsignmentsRow(0,name, seriesId, creator, transferringBody)
    val result = consignmentDao.create(newConsignment)

    result.flatMap(persistedConsignment =>
      seriesService.get(persistedConsignment.seriesId).map(series =>
        Consignment(persistedConsignment.id, persistedConsignment.name, series.get,
          persistedConsignment.creator, persistedConsignment.transferringBody)
      )
    )
  }
}
