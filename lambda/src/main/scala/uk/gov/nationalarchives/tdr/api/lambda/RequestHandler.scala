package uk.gov.nationalarchives.tdr.api.lambda

import java.io.{InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import io.circe.generic.auto._
import io.circe.parser.decode
import uk.gov.nationalarchives.tdr.api.core.{GraphQlRequest, GraphQlServer}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io.Source

class RequestHandler extends RequestStreamHandler {
  override def handleRequest(inputStream: InputStream, outputStream: OutputStream, context: Context): Unit = {
    val inputString = Source.fromInputStream(inputStream, "UTF-8").mkString

    val parsedRequest = decode[GraphQlRequest](inputString)

    val futureResponse = parsedRequest match {
      case Left(failure) => throw new RuntimeException(failure)
      case Right(graphQlRequest) =>
        val response = GraphQlServer.send(graphQlRequest)
        response.map { json =>
          outputStream.write(json.toString.getBytes)
        }
    }

    Await.result(futureResponse, Duration.Inf)
  }
}
