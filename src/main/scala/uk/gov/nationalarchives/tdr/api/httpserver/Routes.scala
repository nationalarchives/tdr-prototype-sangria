package uk.gov.nationalarchives.tdr.api.httpserver

import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Json
import io.circe.generic.auto._
import pdi.jwt.{Jwt, JwtAlgorithm}
import uk.gov.nationalarchives.tdr.api.core.GraphQlRequest

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait Routes extends FailFastCirceSupport {

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[Routes])

  def graphQlActor: ActorRef

  // Required by the `ask` (?) method below
  private implicit lazy val timeout: Timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  private def auth0Authenticator: Authenticator[String] = {
    // TODO: Look for OAuth2BearerToken instead of Credentials.Provided
    case credentials@Credentials.Provided(t) if verifyToken(credentials) => Some(t)
    case _ => None
  }

  // TODO: This should be fetched from Auth0. See https://auth0.com/docs/tokens/guides/jwt/validate-jwt#confirm-that-the-token-is-correctly-signed-using-the-proper-key
  private lazy val auth0PublicCert = "-----BEGIN CERTIFICATE-----\nMIIDFzCCAf+gAwIBAgIJR3mHwSbpLbHlMA0GCSqGSIb3DQEBCwUAMCkxJzAlBgNV\nBAMTHnRuYS10ZHItcHJvdG90eXBlLmV1LmF1dGgwLmNvbTAeFw0xOTExMDQwOTQz\nMzRaFw0zMzA3MTMwOTQzMzRaMCkxJzAlBgNVBAMTHnRuYS10ZHItcHJvdG90eXBl\nLmV1LmF1dGgwLmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAOJN\nF5il0k6mbR4/ZwhV0yBsylh2/uHlY6NqXdTyXeui/mV0sHXn2GE5oDp3XBA6+B2Q\n5oAPrQyy/xQmwGga4WwYrF/c5PyetGhfX3lk6VAG2R35SKZNyQb0wGF+YT5oyhhv\n1USerbusXJWWwvUwYffSJ4JP46ESSalUFys9GbyZjGW2d7tTUNPb+BnpJwH9uA9r\nCwQOzVwjuTZrB9Uull1jmT98lqUsSEWqDG6hwDnMcVfGH6Le/Wy09vhJhfy7dgmt\nHcLEMbNk15yxItA1ATXJAFppHXQtlemtayn3M4KZ20/0KQBI2856EMEPTaO18TjX\nYK+gFOtGxv1eFbWguDcCAwEAAaNCMEAwDwYDVR0TAQH/BAUwAwEB/zAdBgNVHQ4E\nFgQUAGyr51oVyU4MrTCUGVs72tVb8DYwDgYDVR0PAQH/BAQDAgKEMA0GCSqGSIb3\nDQEBCwUAA4IBAQDdAEgZVHDWW2mAnXuuxQXmpNlZWHrvpXQ4CJHlLOtnNmiIYMGi\nloifmNhbFuHbxDewlKtiTnZNjKA7QJKx1DDz8IobZMs7WHGQiCsmin+Yc2Yu5QGB\nI9P+w+azTDMmzuMN1NcoRk5kGCyVbtlM7XscKsXhKm9n3qSNMi75pEPPEI/XYdKA\n6F3Ek4giaVikyHDYzXy8L8Uu7/jQj9StxUS3xCw9EhFZb5oCwchgXx8uH0AKB9tc\n7InRVRm4Tu/uyJ9RjmR2khv86nBd9u5PbU4MaUbn78z1HWqWlJKosI+ZEWxf9leq\n1hrNz+5Ys9R6iC1FKbXmolIuADvssrG4S578\n-----END CERTIFICATE-----"

  private def verifyToken(token: Credentials.Provided): Boolean = {
    val certStream = new ByteArrayInputStream(auth0PublicCert.getBytes)
    val certificate = CertificateFactory.getInstance("X.509").generateCertificate(certStream)
    val publicKey = certificate.getPublicKey

    val decodingResult = Jwt.decodeRaw(token.identifier, publicKey, Seq(JwtAlgorithm.RS256))

    decodingResult match {
      case Success(decoded) => {
        println(s"Decoded token: '$decoded'")
        true
      }
      case Failure(e) => {
        println(s"Failed to decode token '$token'")
        print(e)
        false
      }
    }
  }

  //Use CORS' default settings to get local development to work.
  //Settings can be overridden using conf file or in code: https://github.com/lomigmegard/akka-http-cors
  lazy val graphQlRoutes: Route = cors() {
    authenticateOAuth2("someRealm", auth0Authenticator) { info =>
      pathPrefix("") {
        post {
          entity(as[GraphQlRequest]) {
            graphQlRequest => {
              val graphQlResponse: Future[Json] = (graphQlActor ? graphQlRequest).mapTo[Json]
              complete(graphQlResponse)
            }
          }
        }
      }
    }
  }
}
