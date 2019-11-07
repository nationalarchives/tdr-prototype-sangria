package uk.gov.nationalarchives.tdr.api.core.db

import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import slick.jdbc.PostgresProfile.api._

object DbConnection {
  private val connectionParameters = sys.env.get("TDR_API_ENVIRONMENT") match {
    case Some("TEST") => PrototypeDbConfig
    case _ => DevDbConfig
  }

  private val postgresConfig = ConfigFactory.empty()
    .withValue("db.url", ConfigValueFactory.fromAnyRef(connectionParameters.url))
    .withValue("db.user", ConfigValueFactory.fromAnyRef(connectionParameters.username))
    .withValue("db.password", ConfigValueFactory.fromAnyRef(connectionParameters.password))

  val db = Database.forConfig("db", postgresConfig, new org.postgresql.Driver)
}
