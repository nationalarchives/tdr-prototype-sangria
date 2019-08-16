package uk.gov.nationalarchives.graphql.service

import uk.gov.nationalarchives.Series
import uk.gov.nationalarchives.db.dao.SeriesDao

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SeriesService(seriesDao: SeriesDao) {
  // TODO: This is a repetitive way of getting the series, and it suffers from the N+1 problem. Replace it with GraphQL
  // deferred resolvers.
  def get(id: Int): Future[Option[Series]] = {
    seriesDao.get(id).map(_.map(series =>
      Series(series.id.get, series.name, series.description)
    ))
  }
}
