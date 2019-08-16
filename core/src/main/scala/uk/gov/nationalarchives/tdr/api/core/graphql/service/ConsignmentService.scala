package uk.gov.nationalarchives.tdr.api.core.graphql.service

import uk.gov.nationalarchives.tdr.api.core.db.dao.ConsignmentDao
import uk.gov.nationalarchives.tdr.api.core.db.model.ConsignmentRow
import uk.gov.nationalarchives.tdr.api.core
import uk.gov.nationalarchives.tdr.api.core.Consignment

import scala.concurrent.{ExecutionContext, Future}

class ConsignmentService(consignmentDao: ConsignmentDao, seriesService: SeriesService)(implicit val executionContext: ExecutionContext) {

  def all: Future[Seq[Consignment]] = {
    consignmentDao.all.flatMap(consignmentRows => {
      val consignments = consignmentRows.map(consignmentRow =>
        seriesService.get(consignmentRow.seriesId).map(series =>
          core.Consignment(consignmentRow.id.get, consignmentRow.name, series.get)
        )
      )
      Future.sequence(consignments)
    })
  }

  def get(id: Int): Future[Option[Consignment]] = {
    consignmentDao.get(id).flatMap(_.map(consignmentRow =>
      seriesService.get(consignmentRow.seriesId).map(series =>
        core.Consignment(consignmentRow.id.get, consignmentRow.name, series.get)
      )
    ) match {
      case Some(f) => f.map(Some(_))
      case None => Future.successful(None)
    })
  }

  def create(name: String, seriesId: Int): Future[Consignment] = {
    val newConsignment = ConsignmentRow(None, name, seriesId)
    val result = consignmentDao.create(newConsignment)

    result.flatMap(persistedConsignment =>
      seriesService.get(persistedConsignment.seriesId).map(series =>
        core.Consignment(persistedConsignment.id.get, persistedConsignment.name, series.get)
      )
    )
  }
}
