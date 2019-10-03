package uk.gov.nationalarchives.tdr.api.core.db.dao

import slick.jdbc.PostgresProfile.api._
import uk.gov.nationalarchives.tdr.api.core.db.DbConnection
import uk.gov.nationalarchives.tdr.api.core.db.Tables._


import scala.concurrent.{ExecutionContext, Future}

class ConsignmentDao(implicit val executionContext: ExecutionContext) {
  private val db = DbConnection.db

  private val insertQuery = Consignments returning Consignments.map(_.id) into ((Consignments, id) => Consignments.copy(id =id))

  def all: Future[Seq[ConsignmentsRow]] = {
    db.run(Consignments.result)
  }

  def get(id: Int): Future[Option[ConsignmentsRow]] = {
    db.run(Consignments.filter(_.id === id).result).map(_.headOption)
  }

  def create(consignment: ConsignmentsRow): Future[ConsignmentsRow] = {

    db.run(insertQuery += consignment)
  }
}


