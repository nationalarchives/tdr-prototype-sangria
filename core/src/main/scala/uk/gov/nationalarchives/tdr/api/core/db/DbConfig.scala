package uk.gov.nationalarchives.tdr.api.core.db

import software.amazon.awssdk.services.ssm.SsmClient
import software.amazon.awssdk.services.ssm.model.GetParametersRequest

import scala.collection.JavaConverters._

trait DbConfig {
  def url: String
  def username: String
  def password: String
}

object DevDbConfig extends DbConfig {
  override val url: String = "jdbc:postgresql://localhost/tdrapi"
  override val username: String = "postgres"
  override val password: String = "mysecretpassword"
}

// This config object fetches all the values on startup. This is appropriate in AWS Lambda because we only need the
// values for a short time while the Lambda is running.
//
// If, however, we decide to host the API on longer-running hosting like ECS, we should revisit the config lookup and
// caching.
object PrototypeDbConfig extends DbConfig {

  private val dbUrlParamPath = sys.env("DB_URL_PARAM_PATH")
  private val dbUsernameParamPath = sys.env("DB_USERNAME_PARAM_PATH")
  private val dbPasswordParamPath = sys.env("DB_PASSWORD_PARAM_PATH")

  private val ssmClient = SsmClient.create

  private val request = GetParametersRequest.builder
    .names(dbUrlParamPath, dbUsernameParamPath, dbPasswordParamPath)
    .build
  private val response = ssmClient.getParameters(request)

  private val parameters = response.parameters.asScala.map(param => (param.name, param.value)).toMap

  override def url: String = parameters(dbUrlParamPath)
  override def username: String = parameters(dbUsernameParamPath)
  override def password: String = parameters(dbPasswordParamPath)
}
