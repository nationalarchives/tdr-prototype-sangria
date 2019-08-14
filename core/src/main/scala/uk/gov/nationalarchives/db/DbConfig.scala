package uk.gov.nationalarchives.db

import software.amazon.awssdk.services.ssm.SsmClient
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest

import scala.collection.JavaConverters._

trait DbConfig {
  def url: String
  def username: String
  def password: String
}

object DevDbConfig extends DbConfig {
  // TODO: Get values from environment variables
  override val url: String = "jdbc:postgresql://localhost/tdrapi"
  override val username: String = "postgres"
  override val password: String = "devdbpassword"
}

// This config object fetches all the values on startup. This is appropriate in AWS Lambda because we only need the
// values for a short time while the Lambda is running.
//
// If, however, we decide to host the API on longer-running hosting like ECS, we should revisit the config lookup and
// caching.
object PrototypeDbConfig extends DbConfig {

  private val ssmClient = SsmClient.create

  private val request = GetParametersByPathRequest.builder.path("/tdr/prototype/api/db").build
  private val response = ssmClient.getParametersByPath(request)

  private val parameters = response.parameters.asScala.map(param => (param.name, param.value)).toMap

  override def url: String = parameters("/tdr/prototype/api/db/url")
  override def username: String = parameters("/tdr/prototype/api/db/username")
  override def password: String = parameters("/tdr/prototype/api/db/password")
}
