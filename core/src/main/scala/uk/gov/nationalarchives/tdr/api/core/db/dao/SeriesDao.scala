package uk.gov.nationalarchives.tdr.api.core.db.dao

import slick.jdbc.PostgresProfile.api._
import slick.lifted.{TableQuery, Tag}
import uk.gov.nationalarchives.tdr.api.core.db.DbConnection
import uk.gov.nationalarchives.tdr.api.core.db.dao.SeriesDao.seriesCollections
import uk.gov.nationalarchives.tdr.api.core.db.model.SeriesRow

import scala.concurrent.{ExecutionContext, Future}

class SeriesDao(implicit val executionContext: ExecutionContext) {
  private val db = DbConnection.db

  private val insertQuery = seriesCollections returning seriesCollections.map(_.id) into ((series, id) => series.copy(id = Some(id)))

  def all: Future[Seq[SeriesRow]] = {
    db.run(seriesCollections.result)
  }

  def get(id: Int): Future[Option[SeriesRow]] = {
    db.run(seriesCollections.filter(_.id === id).result).map(_.headOption)
  }

  def create(series: SeriesRow): Future[SeriesRow] = {
    db.run(insertQuery += series)
  }
}

object SeriesDao {
  val seriesCollections = TableQuery[SeriesTable]
}

class SeriesTable(tag: Tag) extends Table[SeriesRow](tag, "series") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def description = column[String]("description")

  override def * = (id.?, name, description).mapTo[SeriesRow]
}
