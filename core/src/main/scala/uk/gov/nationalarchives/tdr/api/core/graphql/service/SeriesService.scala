package uk.gov.nationalarchives.tdr.api.core.graphql.service

import uk.gov.nationalarchives.tdr.api.core.db.dao.SeriesDao
import uk.gov.nationalarchives.tdr.api.core.graphql.Series

import scala.concurrent.{ExecutionContext, Future}

class SeriesService(seriesDao: SeriesDao)(implicit val executionContext: ExecutionContext) {
  // TODO: This is a repetitive way of getting the series, and it suffers from the N+1 problem. Replace it with GraphQL
  // deferred resolvers.
  def get(id: Int): Future[Option[Series]] = {
    seriesDao.get(id).map(_.map(series =>
      Series(series.id.get, series.name, series.description)
    ))
  }
}
