package uk.gov.nationalarchives.db

import slick.jdbc.PostgresProfile.api._

object DbConnection {
  private val driver = "org.postgresql.Driver"
  // Load PostgreSQL driver into classpath
  Class.forName(driver)

  private val dbConfig = sys.env.get("TDR_API_ENVIRONMENT") match {
    case Some("TEST") => PrototypeDbConfig
    case _ => DevDbConfig
  }

  val db = Database.forURL(
    url = dbConfig.url,
    user = dbConfig.username,
    password = dbConfig.password,
    driver = driver
  )
}
