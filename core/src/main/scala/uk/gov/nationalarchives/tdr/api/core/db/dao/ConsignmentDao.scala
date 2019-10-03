package uk.gov.nationalarchives.tdr.api.core.db.dao

import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ProvenShape, TableQuery}
import uk.gov.nationalarchives.tdr.api.core.db.DbConnection
import uk.gov.nationalarchives.tdr.api.core.db.dao.ConsignmentDao.consignments
import uk.gov.nationalarchives.tdr.api.core.db.dao.SeriesDao.seriesCollections
import uk.gov.nationalarchives.tdr.api.core.db.model.ConsignmentRow

import scala.concurrent.{ExecutionContext, Future}

class ConsignmentDao(implicit val executionContext: ExecutionContext) {

  private val db = DbConnection.db

  private val insertQuery = consignments returning consignments.map(_.id) into ((consignment, id) => consignment.copy(id = Some(id)))

  def all: Future[Seq[ConsignmentRow]] = {
    db.run(consignments.result)
  }

  def get(id: Int): Future[Option[ConsignmentRow]] = {
    db.run(consignments.filter(_.id === id).result).map(_.headOption)
  }

  def get(id: Int, creator: String): Future[Option[ConsignmentRow]]  = {
    db.run(consignments.filter(_.id === id).filter(_.creator === creator).result).map(_.headOption)
  }

  def create(consignment: ConsignmentRow): Future[ConsignmentRow] = {
    db.run(insertQuery += consignment)
  }

  def updateProgress(consignmentId: Int, progress: String): Future[Unit] = {
    val query = for {c <- consignments if c.id === consignmentId } yield c.progress
    val updateAction = query.update(progress)
    db.run(updateAction).map(_ => ())
  }
}

object ConsignmentDao {
  val consignments = TableQuery[ConsignmentsTable]
}

class ConsignmentsTable(tag: Tag) extends Table[ConsignmentRow](tag, "consignments") {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def progress = column[String]("progress")
  def seriesId = column[Int]("series_id")
  def creator = column[String]("creator")
  def transferringBody = column[String] ("transferring_body")
  def series = foreignKey("consignment_series_fk", seriesId, seriesCollections)(_.id)

  override def * : ProvenShape[ConsignmentRow] = (id.?, name, progress, seriesId, creator, transferringBody).mapTo[ConsignmentRow]
}