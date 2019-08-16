package uk.gov.nationalarchives.graphql.service

import uk.gov.nationalarchives.Consignment
import uk.gov.nationalarchives.db.dao.ConsignmentDao
import uk.gov.nationalarchives.db.model.ConsignmentRow

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ConsignmentService(consignmentDao: ConsignmentDao, seriesService: SeriesService) {

  def all: Future[Seq[Consignment]] = {
    consignmentDao.all.flatMap(consignmentRows => {
      val consignments = consignmentRows.map(consignmentRow =>
        seriesService.get(consignmentRow.seriesId).map(series =>
          Consignment(consignmentRow.id.get, consignmentRow.name, series.get)
        )
      )
      Future.sequence(consignments)
    })
  }

  def get(id: Int): Future[Option[Consignment]] = {
    consignmentDao.get(id).flatMap(_.map(consignmentRow =>
      seriesService.get(consignmentRow.seriesId).map(series =>
        Consignment(consignmentRow.id.get, consignmentRow.name, series.get)
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
        Consignment(persistedConsignment.id.get, persistedConsignment.name, series.get)
      )
    )
  }
}
