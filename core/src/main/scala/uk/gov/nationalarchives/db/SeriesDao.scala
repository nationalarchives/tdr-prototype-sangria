package uk.gov.nationalarchives.db

import slick.jdbc.PostgresProfile.api._
import slick.lifted.{TableQuery, Tag}
import uk.gov.nationalarchives.model.SeriesDbData
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class SeriesDao {

  // TODO: Commonise with ConsignmentDao
  // Load PostgreSQL driver into classpath
  Class.forName("org.postgresql.Driver")

  private val dbConfig = sys.env.get("TDR_API_ENVIRONMENT") match {
    case Some("TEST") => PrototypeDbConfig
    case _ => DevDbConfig
  }

  private val db = Database.forURL(
    url = dbConfig.url,
    user = dbConfig.username,
    password = dbConfig.password,
    driver = "org.postgresql.Driver"
  )

  val seriesCollections = TableQuery[SeriesTable]

  def get(id: Int): Future[Option[SeriesDbData]] = {
    db.run(seriesCollections.filter(_.id === id).result).map(_.headOption)
  }
}

class SeriesTable(tag: Tag) extends Table[SeriesDbData](tag, "series") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def description = column[String]("description")

  override def * = (id.?, name, description).mapTo[SeriesDbData]
}
