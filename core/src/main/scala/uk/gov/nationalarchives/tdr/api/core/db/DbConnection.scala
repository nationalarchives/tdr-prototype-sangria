package uk.gov.nationalarchives.tdr.api.core.db

import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import slick.jdbc.PostgresProfile.api._

object DbConnection {
  private val dbConfig = sys.env.get("TDR_API_ENVIRONMENT") match {
    case Some("TEST") => PrototypeDbConfig
    case _ => DevDbConfig
  }

  val config = ConfigFactory.empty()
    .withValue("db.url", ConfigValueFactory.fromAnyRef(dbConfig.url))
    .withValue("db.user", ConfigValueFactory.fromAnyRef(dbConfig.username))
    .withValue("db.password", ConfigValueFactory.fromAnyRef(dbConfig.password))

  val db = Database.forConfig("db", config, new org.postgresql.Driver)

}
