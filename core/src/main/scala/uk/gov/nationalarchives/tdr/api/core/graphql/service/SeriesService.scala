package uk.gov.nationalarchives.tdr.api.core.graphql.service

import uk.gov.nationalarchives.tdr.api.core.db.dao.SeriesDao
import uk.gov.nationalarchives.tdr.api.core.db.model.SeriesRow
import uk.gov.nationalarchives.tdr.api.core.graphql.{Consignment, CreateSeriesInput, File, Series}

import scala.concurrent.{ExecutionContext, Future}

class SeriesService(seriesDao: SeriesDao)(implicit val executionContext: ExecutionContext) {

  def all = {
    seriesDao.all.map(seriesRows => {
      seriesRows.map(seriesRow => {
        Series(seriesRow.id.get, seriesRow.name, seriesRow.description)
      })
    })
  }

  // TODO: This is a repetitive way of getting the series, and it suffers from the N+1 problem. Replace it with GraphQL
  // deferred resolvers.
  def get(id: Int): Future[Option[Series]] = {
    seriesDao.get(id).map(_.map(series =>
      Series(series.id.get, series.name, series.description)
    ))
  }

  def get(id: Int, creator: String): Future[Option[Series]] = {
    seriesDao.get(id, creator).map(_.map(series =>
      Series(series.id.get, series.name, series.description)
    ))
  }

  def create(input: CreateSeriesInput): Future[Series] = {
    val newSeries = SeriesRow(None, input.name, input.description)
    val result = seriesDao.create(newSeries)

    result.map(persistedSeries => {
      Series(persistedSeries.id.get, persistedSeries.name, persistedSeries.description)
    })
  }
}
